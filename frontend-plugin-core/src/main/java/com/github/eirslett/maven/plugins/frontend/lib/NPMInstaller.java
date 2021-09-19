package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import com.vdurmont.semver4j.Requirement;
import com.vdurmont.semver4j.Semver;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NPMInstaller {

    private static final String VERSION = "version";

    public static final String DEFAULT_NPM_DOWNLOAD_ROOT = "https://registry.npmjs.org/npm/-/";

    private static final Object LOCK = new Object();

    private String nodeVersion, npmVersion, npmDownloadRoot, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    private Requirement npmVersionRequirement;

    NPMInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public NPMInstaller setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
        return this;
    }

    public NPMInstaller setNpmVersion(String npmVersion) {
        this.npmVersion = npmVersion;
        return this;
    }

    public NPMInstaller setNpmDownloadRoot(String npmDownloadRoot) {
        this.npmDownloadRoot = npmDownloadRoot;
        return this;
    }

    public NPMInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public NPMInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    private boolean npmProvided() throws InstallationException {
        if ("provided".equals(this.npmVersion)) {
            if (Integer.parseInt(this.nodeVersion.replace("v", "").split("[.]")[0]) < 4) {
                throw new InstallationException(
                    "NPM version is '" + this.npmVersion + "' but Node didn't include NPM prior to v4.0.0");
            }
            return true;
        }
        return false;

    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (this.npmDownloadRoot == null || this.npmDownloadRoot.isEmpty()) {
                this.npmDownloadRoot = DEFAULT_NPM_DOWNLOAD_ROOT;
            }
            if ("engines".equals(this.npmVersion)) {
                try {
                    File packageFile = new File(this.config.getWorkingDirectory(), "package.json");
                    HashMap<String, Object> data = new ObjectMapper().readValue(packageFile, HashMap.class);
                    if (data.containsKey("engines")) {
                        HashMap<String, Object> engines = (HashMap<String, Object>) data.get("engines");
                        if (engines.containsKey("npm")) {
                            this.npmVersionRequirement = Requirement.buildNPM((String) engines.get("npm"));
                        } else {
                            this.logger.info("Could not read npm from engines from package.json");
                        }
                    } else {
                        this.logger.info("Could not read engines from package.json");
                    }
                } catch (IOException e) {
                    throw new InstallationException("Could not read npm engine version from package.json", e);
                }
            }

            if (!npmProvided() && !npmIsAlreadyInstalled()) {
                if (this.npmVersionRequirement != null) {
                    // download available node versions
                    try {
                        String downloadUrl = this.npmDownloadRoot
                                + "..";

                        File archive = File.createTempFile("npm_versions", ".json");

                        downloadFile(downloadUrl, archive, this.userName, this.password);

                        HashMap<String, Object> data = new ObjectMapper().readValue(archive, HashMap.class);

                        List<String> npmVersions = new LinkedList<>();
                        if (data.containsKey("versions")) {
                            HashMap<String, Object> versions = (HashMap<String, Object>) data.get("versions");
                            npmVersions.addAll(versions.keySet());
                        } else {
                            this.logger.info("Could not read versions from NPM registry");
                        }

                        logger.debug("Available NPM versions: {}", npmVersions);
                        this.npmVersion = npmVersions.stream().filter(version -> npmVersionRequirement.isSatisfiedBy(new Semver(version, Semver.SemverType.NPM))).findFirst().orElseThrow(() -> new InstallationException("Could not find matching node version satisfying requirement " + this.npmVersionRequirement));
                        this.logger.info("Found matching NPM version {} satisfying requirement {}.", this.npmVersion, this.npmVersionRequirement);
                    } catch (IOException | DownloadException e) {
                        throw new InstallationException("Could not get available node versions.", e);
                    }
                }
                installNpm();
            }
            copyNpmScripts();
        }
    }

    private boolean npmIsAlreadyInstalled() {
        try {
            final File npmPackageJson = new File(
                this.config.getInstallDirectory() + Utils.normalize("/node/node_modules/npm/package.json"));
            if (npmPackageJson.exists()) {
                HashMap<String, Object> data = new ObjectMapper().readValue(npmPackageJson, HashMap.class);
                if (data.containsKey(VERSION)) {
                    final String foundNpmVersion = data.get(VERSION).toString();
                    if (npmVersionRequirement != null && npmVersionRequirement.isSatisfiedBy(new Semver(foundNpmVersion, Semver.SemverType.NPM))) {
                        //update version with installed version
                        this.nodeVersion = foundNpmVersion;
                        this.logger.info("NPM {} matches required version range {} installed.", foundNpmVersion, npmVersionRequirement);
                        return true;
                    } else if (foundNpmVersion.equals(this.npmVersion)) {
                        this.logger.info("NPM {} is already installed.", foundNpmVersion);
                        return true;
                    } else {
                        this.logger.info("NPM {} was installed, but we need version {}", foundNpmVersion,
                            this.npmVersion);
                        return false;
                    }
                } else {
                    this.logger.info("Could not read NPM version from package.json");
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not read package.json", ex);
        }
    }

    private void installNpm() throws InstallationException {
        try {
            this.logger.info("Installing npm version {}", this.npmVersion);
            final String downloadUrl = this.npmDownloadRoot + "npm-" + this.npmVersion + ".tgz";

            CacheDescriptor cacheDescriptor = new CacheDescriptor("npm", this.npmVersion, "tar.gz");

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            File installDirectory = getNodeInstallDirectory();
            File nodeModulesDirectory = new File(installDirectory, "node_modules");

            // We need to delete the existing npm directory first so we clean out any old files, and
            // so we can rename the package directory below.
            File oldNpmDirectory = new File(installDirectory, "npm");
            File npmDirectory = new File(nodeModulesDirectory, "npm");
            try {
                if (oldNpmDirectory.isDirectory()) {
                    FileUtils.deleteDirectory(oldNpmDirectory);
                }
                FileUtils.deleteDirectory(npmDirectory);
            } catch (IOException e) {
                this.logger.warn("Failed to delete existing NPM installation.");
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
            if (packageDirectory.exists() && !npmDirectory.exists()) {
                if (!packageDirectory.renameTo(npmDirectory)) {
                    this.logger.warn("Cannot rename NPM directory, making a copy.");
                    FileUtils.copyDirectory(packageDirectory, npmDirectory);
                }
            }

            this.logger.info("Installed npm locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download npm", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the npm archive", e);
        } catch (IOException e) {
            throw new InstallationException("Could not copy npm", e);
        }
    }

    private void copyNpmScripts() throws InstallationException{
        File installDirectory = getNodeInstallDirectory();

        File nodeModulesDirectory = new File(installDirectory, "node_modules");
        File npmDirectory = new File(nodeModulesDirectory, "npm");
        // create a copy of the npm scripts next to the node executable
        for (String script : Arrays.asList("npm", "npm.cmd", "npx", "npx.cmd")) {
            File scriptFile = new File(npmDirectory, "bin" + File.separator + script);
            if (scriptFile.exists()) {
                File copy = new File(installDirectory, script);
                if (!copy.exists()) {
                    try
                    {
                        FileUtils.copyFile(scriptFile, copy);
                    }
                    catch (IOException e)
                    {
                        throw new InstallationException("Could not copy npm", e);
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
