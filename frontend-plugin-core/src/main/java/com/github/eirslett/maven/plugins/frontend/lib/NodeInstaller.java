package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeInstaller {

    public static final String INSTALL_PATH = "/node";

    public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";

    private static final Object LOCK = new Object();

    private String npmVersion, nodeVersion, nodeDownloadRoot, userName, password;

    private final Logger logger;

    private final List<InstallConfig> configs;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    NodeInstaller(List<InstallConfig> configs, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.configs = configs;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public NodeInstaller setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
        return this;
    }

    public NodeInstaller setNodeDownloadRoot(String nodeDownloadRoot) {
        this.nodeDownloadRoot = nodeDownloadRoot;
        return this;
    }

    public NodeInstaller setNpmVersion(String npmVersion) {
        this.npmVersion = npmVersion;
        return this;
    }

    public NodeInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public NodeInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    private boolean npmProvided() throws InstallationException {
        if (this.npmVersion != null) {
            if ("provided".equals(this.npmVersion)) {
                if (Integer.parseInt(this.nodeVersion.replace("v", "").split("[.]")[0]) < 4) {
                    throw new InstallationException("NPM version is '"
                            + this.npmVersion
                            + "' but Node didn't include NPM prior to v4.0.0");
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (this.nodeDownloadRoot == null || this.nodeDownloadRoot.isEmpty()) {
                this.nodeDownloadRoot = DEFAULT_NODEJS_DOWNLOAD_ROOT;
            }
            //Execution platform always 1st.
            InstallConfig execConfig = this.configs.get(0);
            for (int i = 0; i < this.configs.size(); ++i) {
                processConfig(execConfig, this.configs.get(i));
            }
        }
    }

    private void processConfig(InstallConfig execConfig, InstallConfig config) throws InstallationException {
        if(!nodeIsAlreadyInstalled(config)) {
            this.logger.info("Installing node version {}", this.nodeVersion);
            if (!this.nodeVersion.startsWith("v")) {
                this.logger.warn("Node version does not start with naming convention 'v'.");
            }
            if (config.getPlatform().isWindows()) {
                if (npmProvided()) {
                    installNodeWithNpmForWindows(execConfig, config);
                } else {
                    installNodeForWindows(execConfig, config);
                }
            } else {
                installNodeDefault(execConfig, config);
            }
        }
    }

    private boolean nodeIsAlreadyInstalled(InstallConfig execConfig) {
        try {
            NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(execConfig);
            File nodeFile = executorConfig.getNodePath();
            if (nodeFile.exists()) {
                final String version = new NodeExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult(logger);

                if (version.equals(this.nodeVersion)) {
                    this.logger.info("Node {} is already installed.", version);
                    return true;
                } else {
                    this.logger.info(
                            "Node {} was installed, but we need version {}",
                            version,
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

    private void installNodeDefault(InstallConfig execConfig, InstallConfig config) throws InstallationException {
        try {
            final String longNodeFilename = config.getPlatform().getLongNodeFilename(this.nodeVersion, false);
            String downloadUrl = this.nodeDownloadRoot
                    + config.getPlatform().getNodeDownloadFilename(this.nodeVersion, false);
            String classifier = config.getPlatform().getNodeClassifier();

            File tmpDirectory = getTempDirectory(execConfig);

            CacheDescriptor cacheDescriptor = new CacheDescriptor("node",
                    this.nodeVersion,
                    classifier,
                    config.getPlatform().getArchiveExtension());

            File archive = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            extractFile(archive, tmpDirectory);

            // Search for the node binary
            File nodeBinary = new File(tmpDirectory, longNodeFilename + File.separator + "bin" + File.separator + "node");
            if (!nodeBinary.exists()) {
                throw new FileNotFoundException(
                        "Could not find the downloaded Node.js binary in " + nodeBinary);
            } else {
                File destinationDirectory = getInstallDirectory(execConfig);

                File destination = new File(destinationDirectory, "node");
                if(destination.exists()) {
                    //Building multiple times on windows with file without extention will not be able to rename.
                    destination.delete();
                }
                this.logger.info("Copying node binary from {} to {}", nodeBinary, destination);
                if (!nodeBinary.renameTo(destination)) {
                    throw new InstallationException("Could not install Node: Was not allowed to rename "
                            + nodeBinary
                            + " to "
                            + destination);
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

    private void installNodeWithNpmForWindows(InstallConfig execConfig, InstallConfig config) throws InstallationException {
        try {
            final String longNodeFilename = config.getPlatform().getLongNodeFilename(this.nodeVersion, true);
            String downloadUrl = this.nodeDownloadRoot
                    + config.getPlatform().getNodeDownloadFilename(this.nodeVersion, true);
            String classifier = config.getPlatform().getNodeClassifier();

            File tmpDirectory = getTempDirectory(config);

            CacheDescriptor cacheDescriptor = new CacheDescriptor("node",
                    this.nodeVersion,
                    classifier,
                    config.getPlatform().getArchiveExtension());

            File archive = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            extractFile(archive, tmpDirectory);

            // Search for the node binary
            File nodeBinary = new File(tmpDirectory, longNodeFilename + File.separator + "node.exe");
            if (!nodeBinary.exists()) {
                throw new FileNotFoundException(
                        "Could not find the downloaded Node.js binary in " + nodeBinary);
            } else {
                File destinationDirectory = getInstallDirectory(config);

                File destination = new File(destinationDirectory, "node.exe");
                this.logger.info("Copying node binary from {} to {}", nodeBinary, destination);
                if (!nodeBinary.renameTo(destination)) {
                    throw new InstallationException("Could not install Node: Was not allowed to rename "
                            + nodeBinary
                            + " to "
                            + destination);
                }

                if ("provided".equals(this.npmVersion)) {
                    File tmpNodeModulesDir = new File(tmpDirectory, longNodeFilename + File.separator + "node_modules");
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

    private void installNodeForWindows(InstallConfig execConfig, InstallConfig config) throws InstallationException {
        final String downloadUrl = this.nodeDownloadRoot
                + config.getPlatform().getNodeDownloadFilename(this.nodeVersion, false);
        try {
            File destinationDirectory = getInstallDirectory(config);

            File destination = new File(destinationDirectory, "node.exe");

            String classifier = config.getPlatform().getNodeClassifier();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("node", this.nodeVersion, classifier, "exe");

            File binary = config.getCacheResolver().resolve(cacheDescriptor);

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

    private File getTempDirectory(InstallConfig execConfig) {
        File tmpDirectory = new File(getInstallDirectory(execConfig), "tmp");
        if (!tmpDirectory.exists()) {
            this.logger.debug("Creating temporary directory {}", tmpDirectory);
            tmpDirectory.mkdirs();
        }
        return tmpDirectory;
    }

    private File getInstallDirectory(InstallConfig config) {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
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
