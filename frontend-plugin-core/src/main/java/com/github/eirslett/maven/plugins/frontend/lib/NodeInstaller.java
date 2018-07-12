package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class NodeInstaller {

    public static final String INSTALL_PATH = "/node";

    public static final String NODEJS_DOWNLOAD_ROOT;

    public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";

    private static final String CHINESE_NODEJS_DOWNLOAD_ROOT = "https://npm.taobao.org/mirrors/node/";

    static {
        if (Locale.getDefault().getCountry().equals("CN")) {
            NODEJS_DOWNLOAD_ROOT = CHINESE_NODEJS_DOWNLOAD_ROOT;
        } else {
            NODEJS_DOWNLOAD_ROOT = DEFAULT_NODEJS_DOWNLOAD_ROOT;
        }
    }

    private static final Object LOCK = new Object();

    private String npmVersion, nodeVersion, nodeDownloadRoot, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    NodeInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
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

    /**
     * Returns true if npm is provided in node.
     * Which means node version should be greater than 4.0.0 and npm version should be "provided".
     */
    private boolean npmProvided() throws InstallationException {
        if (this.npmVersion != null) {
            if ("provided".equals(this.npmVersion)) {
                if (Integer.parseInt(this.nodeVersion.replace("v", "").split("[.]")[0]) < 4) {
                    throw new InstallationException("NPM version is '" + this.npmVersion
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
                this.nodeDownloadRoot = NODEJS_DOWNLOAD_ROOT;
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
        }
    }

    private boolean nodeIsAlreadyInstalled() {
        try {
            NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(this.config);
            File nodeFile = executorConfig.getNodePath();
            if (nodeFile.exists()) {
                final String version =
                        new NodeExecutor(executorConfig, Collections.singletonList("--version"), null)
                                .executeAndGetResult(logger);

                if (version.equals(this.nodeVersion)) {
                    this.logger.info("Node {} is already installed.", version);
                    return true;
                } else {
                    this.logger.info("Node {} was installed, but we need version {}", version, this.nodeVersion);
                    return false;
                }
            } else {
                return false;
            }
        } catch (ProcessExecutionException e) {
            return false;
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
                shorttenPath(tmpDirectory.toPath());
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
            if (!tmpDirectory.mkdirs()) {
                this.logger.debug("Temporary directory {} already exists!", tmpDirectory);
            }
        }
        return tmpDirectory;
    }

    private File getInstallDirectory() {
        File installDirectory = new File(this.config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            this.logger.debug("Creating install directory {}", installDirectory);
            if (!installDirectory.mkdirs()) {
                this.logger.debug("Install directory {} already exists!", installDirectory);
            }
        }
        return installDirectory;
    }

    private void deleteTempDirectory(File tmpDirectory) throws IOException {
        if (tmpDirectory != null && tmpDirectory.exists()) {
            this.logger.debug("Deleting temporary directory {}", tmpDirectory);
            deleteTempDirectory(tmpDirectory.toPath());
        }
    }

    /**
     * Recursively delete a directory using java.nio.
     * Since FileUtils.deleteDirectory is not very good in practice
     */
    private void deleteTempDirectory(Path tmpDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(tmpDirectory)) {
            paths.map(Path::toFile)
                    .sorted((o1, o2) -> -o1.compareTo(o2))
                    .forEach(File::delete);
        }
    }

    /**
     * Move up to shortten all the long paths in the base dir.
     * It will cause problems when manipulating long path on Windows.
     */
    private void shorttenPath(Path base) {
        try {
            Optional<Path> longPath = findLongPath(base);
            while (longPath.isPresent()) {
                Files.move(longPath.get(), base.resolve(String.valueOf(System.currentTimeMillis())));
                longPath = findLongPath(base);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Walk through a base Path and find any long paths if exists.
     * A long path is a path whose string representation is longer than 247 chars.
     */
    private Optional<Path> findLongPath(Path base) throws IOException {
        try (Stream<Path> paths = Files.walk(base)) {
            return paths.filter(path -> path.toString().length() > 247).findAny();
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
