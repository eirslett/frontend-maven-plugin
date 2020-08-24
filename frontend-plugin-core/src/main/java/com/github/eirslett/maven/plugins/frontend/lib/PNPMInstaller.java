package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PNPMInstaller {

    private static final String VERSION = "version";

    public static final String DEFAULT_PNPM_DOWNLOAD_ROOT = "https://registry.npmjs.org/pnpm/-/";

    private static final Object LOCK = new Object();

    private String pnpmVersion, pnpmDownloadRoot, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    PNPMInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public PNPMInstaller setNodeVersion(String nodeVersion) {
        return this;
    }

    public PNPMInstaller setPnpmVersion(String pnpmVersion) {
        this.pnpmVersion = pnpmVersion;
        return this;
    }

    public PNPMInstaller setPnpmDownloadRoot(String pnpmDownloadRoot) {
        this.pnpmDownloadRoot = pnpmDownloadRoot;
        return this;
    }

    public PNPMInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public PNPMInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (this.pnpmDownloadRoot == null || this.pnpmDownloadRoot.isEmpty()) {
                this.pnpmDownloadRoot = DEFAULT_PNPM_DOWNLOAD_ROOT;
            }
            if (!pnpmIsAlreadyInstalled()) {
                installPnpm();
            }
            copyPnpmScripts();
        }
    }

    private boolean pnpmIsAlreadyInstalled() {
        try {
            final File pnpmPackageJson = new File(
                this.config.getInstallDirectory() + Utils.normalize("/node/node_modules/pnpm/package.json"));
            if (pnpmPackageJson.exists()) {
                HashMap<String, Object> data = new ObjectMapper().readValue(pnpmPackageJson, HashMap.class);
                if (data.containsKey(VERSION)) {
                    final String foundPnpmVersion = data.get(VERSION).toString();
                    if (foundPnpmVersion.equals(this.pnpmVersion)) {
                        this.logger.info("PNPM {} is already installed.", foundPnpmVersion);
                        return true;
                    } else {
                        this.logger.info("PNPM {} was installed, but we need version {}", foundPnpmVersion,
                            this.pnpmVersion);
                        return false;
                    }
                } else {
                    this.logger.info("Could not read PNPM version from package.json");
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not read package.json", ex);
        }
    }

    private void installPnpm() throws InstallationException {
        try {
            this.logger.info("Installing pnpm version {}", this.pnpmVersion);
            String pnpmVersionClean = this.pnpmVersion.replaceFirst("^v(?=[0-9]+)", "");
            final String downloadUrl = this.pnpmDownloadRoot + "pnpm-" + pnpmVersionClean + ".tgz";

            CacheDescriptor cacheDescriptor = new CacheDescriptor("pnpm", pnpmVersionClean, "tar.gz");

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            File installDirectory = getNodeInstallDirectory();
            File nodeModulesDirectory = new File(installDirectory, "node_modules");

            // We need to delete the existing pnpm directory first so we clean out any old files, and
            // so we can rename the package directory below.
            File oldNpmDirectory = new File(installDirectory, "pnpm");
            File pnpmDirectory = new File(nodeModulesDirectory, "pnpm");
            try {
                if (oldNpmDirectory.isDirectory()) {
                    FileUtils.deleteDirectory(oldNpmDirectory);
                }
                FileUtils.deleteDirectory(pnpmDirectory);
            } catch (IOException e) {
                this.logger.warn("Failed to delete existing PNPM installation.");
            }

            File packageDirectory = new File(nodeModulesDirectory, "package");
            try {
                extractFile(archive, nodeModulesDirectory);
            } catch (ArchiveExtractionException e) {
                if (e.getCause() instanceof EOFException) {
                    // https://github.com/eirslett/frontend-maven-plugin/issues/794
                    // The downloading was probably interrupted and archive file is incomplete:
                    // delete it to retry from scratch
                    this.logger.error("The archive file {} is corrupted and will be deleted. "
                            + "Please try the build again.", archive.getPath());
                    archive.delete();
                    if (packageDirectory.exists()) {
                        FileUtils.deleteDirectory(packageDirectory);
                    }
                }

                throw e;
            }

            // handles difference between old and new download root (nodejs.org/dist/npm and
            // registry.npmjs.org)
            // see https://github.com/eirslett/frontend-maven-plugin/issues/65#issuecomment-52024254
            if (packageDirectory.exists() && !pnpmDirectory.exists()) {
                if (!packageDirectory.renameTo(pnpmDirectory)) {
                    this.logger.warn("Cannot rename PNPM directory, making a copy.");
                    FileUtils.copyDirectory(packageDirectory, pnpmDirectory);
                }
            }

            this.logger.info("Installed pnpm locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download pnpm", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the pnpm archive", e);
        } catch (IOException e) {
            throw new InstallationException("Could not copy pnpm", e);
        }
    }

    private void copyPnpmScripts() throws InstallationException{
        File installDirectory = getNodeInstallDirectory();

        File nodeModulesDirectory = new File(installDirectory, "node_modules");
        File pnpmDirectory = new File(nodeModulesDirectory, "pnpm");
        // create a copy of the pnpm scripts next to the node executable
        for (String script : Arrays.asList("pnpm", "pnpm.cmd")) {
            File scriptFile = new File(pnpmDirectory, "bin" + File.separator + script);
            if (scriptFile.exists()) {
                File copy = new File(installDirectory, script);
                if (!copy.exists()) {
                    try
                    {
                        FileUtils.copyFile(scriptFile, copy);
                    }
                    catch (IOException e)
                    {
                        throw new InstallationException("Could not copy pnpm", e);
                    }
                    copy.setExecutable(true);
                }
            }
        }
    }

    private File getNodeInstallDirectory() {
        File installDirectory = new File(this.config.getInstallDirectory(), NodeInstaller.INSTALL_PATH);
        if (!installDirectory.exists()) {
            this.logger.debug("Creating install directory {}", installDirectory);
            installDirectory.mkdirs();
        }
        return installDirectory;
    }

    private void extractFile(File archive, File destinationDirectory) throws ArchiveExtractionException {
        this.logger.info("Unpacking {} into {}", archive, destinationDirectory);
        this.archiveExtractor.extract(archive.getPath(), destinationDirectory.getPath());
    }

    private void downloadFileIfMissing(String downloadUrl, File destination, String userName, String password)
        throws DownloadException {
        if (!destination.exists()) {
            downloadFile(downloadUrl, destination, userName, password);
        }
    }

    private void downloadFile(String downloadUrl, File destination, String userName, String password)
        throws DownloadException {
        this.logger.info("Downloading {} to {}", downloadUrl, destination);
        this.fileDownloader.download(downloadUrl, destination.getPath(), userName, password);
    }
}
