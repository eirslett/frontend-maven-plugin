package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
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

    private boolean yarnIsAlreadyInstalled() {
        try {
            YarnExecutorConfig executorConfig = new InstallYarnExecutorConfig(config);
            File nodeFile = executorConfig.getYarnPath();
            if (nodeFile.exists()) {
                final String version =
                    new YarnExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult();

                if (version.equals(yarnVersion)) {
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
        try {
            logger.info("Installing Yarn version {}", yarnVersion);
            String downloadUrl = yarnDownloadRoot + yarnVersion;
            String fileending;
            if (config.getPlatform().isWindows()) {
                fileending = "/yarn-" + yarnVersion.substring(1, yarnVersion.length()) + ".msi";
            } else {
                fileending = "/yarn-" + yarnVersion + ".tar.gz";
            }
            downloadUrl += fileending;

            CacheDescriptor cacheDescriptor = new CacheDescriptor("yarn", yarnVersion, fileending);

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

            extractFile(archive, installDirectory);

            logger.info("Installed Yarn locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Yarn", e);
        } catch (ArchiveExtractionException e) {
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

    private void extractFile(File archive, File destinationDirectory) throws ArchiveExtractionException {
        logger.info("Unpacking {} into {}", archive, destinationDirectory);
        archiveExtractor.extract(archive.getPath(), destinationDirectory.getPath());
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
