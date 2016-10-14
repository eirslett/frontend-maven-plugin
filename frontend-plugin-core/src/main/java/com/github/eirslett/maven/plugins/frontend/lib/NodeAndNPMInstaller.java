package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeAndNPMInstaller {

    public static final String INSTALL_PATH = "/.buildenv/node";

    private static final String VERSION = "version";

    public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";

    public static final String DEFAULT_NPM_DOWNLOAD_ROOT = "http://registry.npmjs.org/npm/-/";

    private static final Object LOCK = new Object();

    private String nodeVersion, npmVersion, nodeDownloadRoot, npmDownloadRoot, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    NodeAndNPMInstaller(InstallConfig config, ArchiveExtractor archiveExtractor,
        FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public NodeAndNPMInstaller setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
        return this;
    }

    public NodeAndNPMInstaller setNodeDownloadRoot(String nodeDownloadRoot) {
        this.nodeDownloadRoot = nodeDownloadRoot;
        return this;
    }

    public NodeAndNPMInstaller setNpmVersion(String npmVersion) {
        this.npmVersion = npmVersion;
        return this;
    }

    public NodeAndNPMInstaller setNpmDownloadRoot(String npmDownloadRoot) {
        this.npmDownloadRoot = npmDownloadRoot;
        return this;
    }

    public NodeAndNPMInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public NodeAndNPMInstaller setPassword(String password) {
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
            if (this.nodeDownloadRoot == null || this.nodeDownloadRoot.isEmpty()) {
                this.nodeDownloadRoot = DEFAULT_NODEJS_DOWNLOAD_ROOT;
            }
            if (this.npmDownloadRoot == null || this.npmDownloadRoot.isEmpty()) {
                this.npmDownloadRoot = DEFAULT_NPM_DOWNLOAD_ROOT;
            }
            if (!nodeIsAlreadyInstalled()) {
                this.logger.info("Installing node version {}", this.nodeVersion);
                if (!this.nodeVersion.startsWith("v")) {
                    this.logger.warn("Node version does not start with naming convention 'v'.");
                }
                if (this.config.getPlatform().isWindows()) {
                    if (npmProvided()) {
                        installNodeWithNpmForWindows();
                    } else {
                        installNodeForWindows();
                    }
                } else {
                    installNodeDefault();
                }
            }
            if (!npmProvided() && !npmIsAlreadyInstalled()) {
                installNpm();
            }
        }
    }

    private boolean nodeIsAlreadyInstalled() {
        try {
            NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(this.config);
            File nodeFile = executorConfig.getNodePath();
            if (nodeFile.exists()) {
                final String version =
                    new NodeExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult();

                if (version.equals(this.nodeVersion)) {
                    this.logger.info("Node {} is already installed.", version);
                    return true;
                } else {
                    this.logger.info("Node {} was installed, but we need version {}", version,
                        this.nodeVersion);
                    return false;
                }
            } else {
                return false;
            }
        } catch (ProcessExecutionException e) {
            return false;
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
                    if (foundNpmVersion.equals(this.npmVersion)) {
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

            File installDirectory = getInstallDirectory();
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

            extractFile(archive, nodeModulesDirectory);

            // handles difference between old and new download root (nodejs.org/dist/npm and
            // registry.npmjs.org)
            // see https://github.com/eirslett/frontend-maven-plugin/issues/65#issuecomment-52024254
            File packageDirectory = new File(nodeModulesDirectory, "package");
            if (packageDirectory.exists() && !npmDirectory.exists()) {
                if (!packageDirectory.renameTo(npmDirectory)) {
                    this.logger.warn("Cannot rename NPM directory, making a copy.");
                    FileUtils.copyDirectory(packageDirectory, npmDirectory);
                }
            }

            // create a copy of the npm scripts next to the node executable
            for (String script : Arrays.asList("npm", "npm.cmd")) {
                File scriptFile = new File(npmDirectory, "bin" + File.separator + script);
                if (scriptFile.exists()) {
                    File copy = new File(installDirectory, script);
                    FileUtils.copyFile(scriptFile, copy);
                    copy.setExecutable(true);
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

    private void installNodeDefault() throws InstallationException {
        try {
            final String longNodeFilename =
                this.config.getPlatform().getLongNodeFilename(this.nodeVersion, false);
            String downloadUrl = this.nodeDownloadRoot
                + this.config.getPlatform().getNodeDownloadFilename(this.nodeVersion, false);
            String classifier = this.config.getPlatform().getNodeClassifier();

            File tmpDirectory = getTempDirectory();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("node", this.nodeVersion, classifier,
                this.config.getPlatform().getArchiveExtension());

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            extractFile(archive, tmpDirectory);

            // Search for the node binary
            File nodeBinary =
                new File(tmpDirectory, longNodeFilename + File.separator + "bin" + File.separator + "node");
            if (!nodeBinary.exists()) {
                throw new FileNotFoundException(
                    "Could not find the downloaded Node.js binary in " + nodeBinary);
            } else {
                File destinationDirectory = getInstallDirectory();

                File destination = new File(destinationDirectory, "node");
                this.logger.info("Copying node binary from {} to {}", nodeBinary, destination);
                if (!nodeBinary.renameTo(destination)) {
                    throw new InstallationException("Could not install Node: Was not allowed to rename "
                        + nodeBinary + " to " + destination);
                }

                if (!destination.setExecutable(true, false)) {
                    throw new InstallationException(
                        "Could not install Node: Was not allowed to make " + destination + " executable.");
                }

                if (npmProvided()) {
                    File tmpNodeModulesDir = new File(tmpDirectory,
                        longNodeFilename + File.separator + "lib" + File.separator + "node_modules");
                    File nodeModulesDirectory = new File(destinationDirectory, "node_modules");
                    File npmDirectory = new File(nodeModulesDirectory, "npm");
                    FileUtils.copyDirectory(tmpNodeModulesDir, nodeModulesDirectory);
                    this.logger.info("Extracting NPM");
                    // create a copy of the npm scripts next to the node executable
                    for (String script : Arrays.asList("npm", "npm.cmd")) {
                        File scriptFile = new File(npmDirectory, "bin" + File.separator + script);
                        if (scriptFile.exists()) {
                            scriptFile.setExecutable(true);
                        }
                    }
                }

                deleteTempDirectory(tmpDirectory);

                this.logger.info("Installed node locally.");
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install Node", e);
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Node.js", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the Node archive", e);
        }
    }

    private void installNodeWithNpmForWindows() throws InstallationException {
        try {
            final String longNodeFilename =
                this.config.getPlatform().getLongNodeFilename(this.nodeVersion, true);
            String downloadUrl = this.nodeDownloadRoot
                + this.config.getPlatform().getNodeDownloadFilename(this.nodeVersion, true);
            String classifier = this.config.getPlatform().getNodeClassifier();

            File tmpDirectory = getTempDirectory();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("node", this.nodeVersion, classifier,
                this.config.getPlatform().getArchiveExtension());

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            extractFile(archive, tmpDirectory);

            // Search for the node binary
            File nodeBinary = new File(tmpDirectory, longNodeFilename + File.separator + "node.exe");
            if (!nodeBinary.exists()) {
                throw new FileNotFoundException(
                    "Could not find the downloaded Node.js binary in " + nodeBinary);
            } else {
                File destinationDirectory = getInstallDirectory();

                File destination = new File(destinationDirectory, "node.exe");
                this.logger.info("Copying node binary from {} to {}", nodeBinary, destination);
                if (!nodeBinary.renameTo(destination)) {
                    throw new InstallationException("Could not install Node: Was not allowed to rename "
                        + nodeBinary + " to " + destination);
                }

                if ("provided".equals(this.npmVersion)) {
                    File tmpNodeModulesDir =
                        new File(tmpDirectory, longNodeFilename + File.separator + "node_modules");
                    File nodeModulesDirectory = new File(destinationDirectory, "node_modules");
                    FileUtils.copyDirectory(tmpNodeModulesDir, nodeModulesDirectory);
                }
                deleteTempDirectory(tmpDirectory);

                this.logger.info("Installed node locally.");
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install Node", e);
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Node.js", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the Node archive", e);
        }

    }

    private void installNodeForWindows() throws InstallationException {
        final String downloadUrl = this.nodeDownloadRoot
            + this.config.getPlatform().getNodeDownloadFilename(this.nodeVersion, false);
        try {
            File destinationDirectory = getInstallDirectory();

            File destination = new File(destinationDirectory, "node.exe");

            String classifier = this.config.getPlatform().getNodeClassifier();

            CacheDescriptor cacheDescriptor =
                new CacheDescriptor("node", this.nodeVersion, classifier, "exe");

            File binary = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, binary, this.userName, this.password);

            this.logger.info("Copying node binary from {} to {}", binary, destination);
            FileUtils.copyFile(binary, destination);

            this.logger.info("Installed node locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Node.js from: " + downloadUrl, e);
        } catch (IOException e) {
            throw new InstallationException("Could not install Node.js", e);
        }
    }

    private File getTempDirectory() {
        File tmpDirectory = new File(getInstallDirectory(), "tmp");
        if (!tmpDirectory.exists()) {
            this.logger.debug("Creating temporary directory {}", tmpDirectory);
            tmpDirectory.mkdirs();
        }
        return tmpDirectory;
    }

    private File getInstallDirectory() {
        File installDirectory = new File(this.config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            this.logger.debug("Creating install directory {}", installDirectory);
            installDirectory.mkdirs();
        }
        return installDirectory;
    }

    private void deleteTempDirectory(File tmpDirectory) throws IOException {
        if (tmpDirectory != null && tmpDirectory.exists()) {
            this.logger.debug("Deleting temporary directory {}", tmpDirectory);
            FileUtils.deleteDirectory(tmpDirectory);
        }
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
