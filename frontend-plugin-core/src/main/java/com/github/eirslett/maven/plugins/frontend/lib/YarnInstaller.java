package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YarnInstaller {

    public static final String INSTALL_PATH = "/node/yarn";

    public static final String DEFAULT_YARN_DOWNLOAD_ROOT =
        "https://github.com/yarnpkg/yarn/releases/download/";

    private static final Object LOCK = new Object();

    private static final String YARN_ROOT_DIRECTORY = "dist";

    private String yarnVersion, yarnDownloadRoot, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    YarnInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public YarnInstaller setYarnVersion(String yarnVersion) {
        this.yarnVersion = yarnVersion;
        return this;
    }

    public YarnInstaller setYarnDownloadRoot(String yarnDownloadRoot) {
        this.yarnDownloadRoot = yarnDownloadRoot;
        return this;
    }

    public YarnInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public YarnInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (yarnDownloadRoot == null || yarnDownloadRoot.isEmpty()) {
                yarnDownloadRoot = DEFAULT_YARN_DOWNLOAD_ROOT;
            }
            if (!yarnIsAlreadyInstalled()) {
                if (!yarnVersion.startsWith("v")) {
                    throw new InstallationException("Yarn version has to start with prefix 'v'.");
                }
                installYarn();
            }
        }
    }

    public void install(String yarnDownloadUrl, String yarnExtension) throws InstallationException {
        if (yarnVersion == null || yarnVersion.isEmpty()) {
            throw new InstallationException("yarnVersion has to be provided.");
        }
        if (yarnExtension == null || yarnExtension.isEmpty()) {
            throw new InstallationException("yarnExtension has to be provided.");
        }
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (!yarnIsAlreadyInstalled()) {
                installYarn(yarnDownloadUrl, yarnVersion, yarnExtension);
            }
        }
    }

    private boolean yarnIsAlreadyInstalled() {
        try {
            YarnExecutorConfig executorConfig = new InstallYarnExecutorConfig(config);
            File nodeFile = executorConfig.getYarnPath();
            if (nodeFile.exists()) {
                final String version =
                    new YarnExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult(logger).trim();

                if (version.equals(yarnVersion.replaceFirst("^v", ""))) {
                    logger.info("Yarn {} is already installed.", version);
                    return true;
                } else {
                    logger.info("Yarn {} was installed, but we need version {}", version, yarnVersion);
                    return false;
                }
            } else {
                return false;
            }
        } catch (ProcessExecutionException e) {
            return false;
        }
    }

    private void installYarn() throws InstallationException {
        String downloadUrl = yarnDownloadRoot + yarnVersion;
        String defaultYarnExtension = "tar.gz";

        try {
            File installDirectory = getInstallDirectory();
            ensureCorrectYarnRootDirectory(installDirectory, yarnVersion);
        } catch (IOException e) {
            throw new InstallationException("Could not extract the Yarn archive", e);
        }

        installYarn(downloadUrl, yarnVersion, defaultYarnExtension);
    }

    private void installYarn(String yarnDownloadUrl, String yarnVersion, String yarnExtension) throws InstallationException {
        try {
            logger.info("Installing Yarn version {}", yarnVersion);
            String fileEnding = "/yarn-" + yarnVersion + "." + yarnExtension;

            String downloadUrl = yarnDownloadUrl + fileEnding;

            CacheDescriptor cacheDescriptor = new CacheDescriptor("yarn", yarnVersion, yarnExtension);

            File archive = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, userName, password);

            File installDirectory = getInstallDirectory();

            // We need to delete the existing yarn directory first so we clean out any old files, and
            // so we can rename the package directory below.
            try {
                if (installDirectory.isDirectory()) {
                    FileUtils.deleteDirectory(installDirectory);
                }
            } catch (IOException e) {
                logger.warn("Failed to delete existing Yarn installation.");
            }

            try {
                extractFile(archive, installDirectory);
            } catch (ArchiveExtractionException e) {
                if (e.getCause() instanceof EOFException) {
                    // https://github.com/eirslett/frontend-maven-plugin/issues/794
                    // The downloading was probably interrupted and archive file is incomplete:
                    // delete it to retry from scratch
                    this.logger.error("The archive file {} is corrupted and will be deleted. "
                            + "Please try the build again.", archive.getPath());
                    archive.delete();
                    if (installDirectory.exists()) {
                        FileUtils.deleteDirectory(installDirectory);
                    }
                }

                throw e;
            }

            logger.info("Installed Yarn locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Yarn", e);
        } catch (ArchiveExtractionException | IOException e) {
            throw new InstallationException("Could not extract the Yarn archive", e);
        }
    }

    private File getInstallDirectory() {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            logger.debug("Creating install directory {}", installDirectory);
            installDirectory.mkdirs();
        }
        return installDirectory;
    }

    private File getDistDirectory(File destinationDirectory) throws IOException {
        File distDirectory = new File(destinationDirectory,  "dist");
        if (distDirectory.exists()) {
            try {
                FileUtils.deleteDirectory(distDirectory);
            } catch (IOException e) {
                throw new IOException("Could not clean up existing dist directory:  " + distDirectory.getName());
            }
        }
        logger.info("Creating directory {}", distDirectory);
        distDirectory.mkdir();
        return distDirectory;
    }

    private void copyToDist(File destinationDirectory) throws IOException {
        // Copying installed /node/yarn/yarn-<version> to /node/yarn/dist for execution path.
        // See this: https://github.com/eirslett/frontend-maven-plugin/issues/647
        File versionedDirectory = new File(destinationDirectory, "yarn-" +
            (yarnVersion.startsWith("v") ? "" : "v") +
            yarnVersion);
        File distDirectory = getDistDirectory(destinationDirectory);

        if (versionedDirectory.exists()) {
            try {
                logger.info("Copying {} to {}", versionedDirectory, distDirectory);
                FileUtils.copyDirectory(versionedDirectory, distDirectory, true);
                File distYarn = new File(distDirectory, "bin/yarn");
                distYarn.setExecutable(true);
            } catch (IOException e) {
                logger.error("Failed to copy from {} to {}.", versionedDirectory, distDirectory);
            }
        }
    }

    private void extractFile(File archive, File destinationDirectory) throws ArchiveExtractionException, IOException {
        logger.info("Unpacking {} into {}", archive, destinationDirectory);
        archiveExtractor.extract(archive.getPath(), destinationDirectory.getPath());
        copyToDist(destinationDirectory);

    }

    private void ensureCorrectYarnRootDirectory(File installDirectory, String yarnVersion) throws IOException {
        File yarnRootDirectory = new File(installDirectory, YARN_ROOT_DIRECTORY);
        if (!yarnRootDirectory.exists()) {
            logger.debug("Yarn root directory not found, checking for yarn-{}", yarnVersion);
            // Handle renaming Yarn 1.X root to YARN_ROOT_DIRECTORY
            File yarnOneXDirectory = new File(installDirectory, "yarn-" + yarnVersion);
            if (yarnOneXDirectory.isDirectory()) {
                if (!yarnOneXDirectory.renameTo(yarnRootDirectory)) {
                    throw new IOException("Could not rename versioned yarn root directory to " + YARN_ROOT_DIRECTORY);
                }
            } else {
                throw new FileNotFoundException("Could not find yarn distribution directory during extract");
            }
        }
    }

    private void downloadFileIfMissing(String downloadUrl, File destination, String userName, String password)
        throws DownloadException {
        if (!destination.exists()) {
            downloadFile(downloadUrl, destination, userName, password);
        }
    }

    private void downloadFile(String downloadUrl, File destination, String userName, String password)
        throws DownloadException {
        logger.info("Downloading {} to {}", downloadUrl, destination);
        fileDownloader.download(downloadUrl, destination.getPath(), userName, password);
    }
}
