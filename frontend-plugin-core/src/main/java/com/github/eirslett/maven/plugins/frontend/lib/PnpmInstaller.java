package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PnpmInstaller {

    private static final String VERSION = "version";

    public static final String DEFAULT_PNPM_DOWNLOAD_ROOT = "https://registry.npmjs.org/pnpm/-/";

    private static final Object LOCK = new Object();

    private String pnpmVersion, pnpmDownloadRoot, userName, password;
    
    private Map<String, String> httpHeaders;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    PnpmInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public PnpmInstaller setNodeVersion(String nodeVersion) {
        return this;
    }

    public PnpmInstaller setPnpmVersion(String pnpmVersion) {
        this.pnpmVersion = pnpmVersion;
        return this;
    }

    public PnpmInstaller setPnpmDownloadRoot(String pnpmDownloadRoot) {
        this.pnpmDownloadRoot = pnpmDownloadRoot;
        return this;
    }

    public PnpmInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public PnpmInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    public PnpmInstaller setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
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

            if (this.config.getPlatform().isWindows()) {
                linkExecutableWindows();
            } else {
                linkExecutable();
            }
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
                    if (foundPnpmVersion.equals(this.pnpmVersion.replaceFirst("^v", ""))) {
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

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password, httpHeaders);

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

    private void linkExecutable() throws InstallationException{
        File nodeInstallDirectory = getNodeInstallDirectory();
        File pnpmExecutable = new File(nodeInstallDirectory, "pnpm");

        if (pnpmExecutable.exists()) {
            this.logger.info("Existing pnpm executable found, skipping linking.");
            return;
        }

        NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(this.config);
        File pnpmJsExecutable = executorConfig.getPnpmCjsPath();

        if (!pnpmJsExecutable.exists()) {
            throw new InstallationException("Could not link to pnpm executable, no pnpm installation found.");
        }

        this.logger.info("No pnpm executable found, creating symbolic link to {}.", pnpmJsExecutable.toPath());

        try {
            Files.createSymbolicLink(pnpmExecutable.toPath(), pnpmJsExecutable.toPath());
        } catch (IOException e) {
            throw new InstallationException("Could not create symbolic link for pnpm executable.", e);
        }
    }

    private void linkExecutableWindows() throws InstallationException{
        File nodeInstallDirectory = getNodeInstallDirectory();
        File pnpmExecutable = new File(nodeInstallDirectory, "pnpm.cmd");

        if (pnpmExecutable.exists()) {
            this.logger.info("Existing pnpm executable found, skipping linking.");
            return;
        }

        NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(this.config);
        File pnpmJsExecutable = executorConfig.getPnpmCjsPath();

        if (!pnpmJsExecutable.exists()) {
            throw new InstallationException("Could not link to pnpm executable, no pnpm installation found.");
        }

        this.logger.info("No pnpm executable found, creating proxy script to {}.", pnpmJsExecutable.toPath());

        Path nodePath = executorConfig.getNodePath().toPath();
        Path relativeNodePath = nodeInstallDirectory.toPath().relativize(nodePath);
        Path relativePnpmPath = nodeInstallDirectory.toPath().relativize(pnpmJsExecutable.toPath());

        // Create a script that will proxy any commands passed into it to the pnpm executable.
        String scriptContents = new StringBuilder()
                .append(":: Created by frontend-maven-plugin, please don't edit manually.\r\n")
                .append("@ECHO OFF\r\n")
                .append("\r\n")
                .append("SETLOCAL\r\n")
                .append("\r\n")
                .append(String.format("SET \"NODE_EXE=%%~dp0\\%s\"\r\n", relativeNodePath))
                .append(String.format("SET \"PNPM_CLI_JS=%%~dp0\\%s\"\r\n", relativePnpmPath))
                .append("\r\n")
                .append("\"%NODE_EXE%\" \"%PNPM_CLI_JS%\" %*")
                .toString();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(pnpmExecutable));
            writer.write(scriptContents);
            writer.close();
        } catch (IOException e) {
            throw new InstallationException("Could not create proxy script for pnpm executable.", e);
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

    private void downloadFileIfMissing(String downloadUrl, File destination, String userName, String password, Map<String, String> httpHeaders)
        throws DownloadException {
        if (!destination.exists()) {
            downloadFile(downloadUrl, destination, userName, password, httpHeaders);
        }
    }

    private void downloadFile(String downloadUrl, File destination, String userName, String password, Map<String, String> httpHeaders)
        throws DownloadException {
        this.logger.info("Downloading {} to {}", downloadUrl, destination);
        this.fileDownloader.download(downloadUrl, destination.getPath(), userName, password, httpHeaders);
    }
}
