package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YarnInstaller {

    public static final String INSTALL_PATH = "/.buildenv/yarn";

    public static final String DEFAULT_YARN_DOWNLOAD_ROOT =
        "https://github.com/yarnpkg/yarn/releases/download/";

    private static final Object LOCK = new Object();

    private String yarnVersion, yarnDownloadRoot, userName, password;

    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    YarnInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
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
            if (this.yarnDownloadRoot == null || this.yarnDownloadRoot.isEmpty()) {
                this.yarnDownloadRoot = DEFAULT_YARN_DOWNLOAD_ROOT;
            }
            if (!yarnIsAlreadyInstalled()) {
                if (!this.yarnVersion.startsWith("v")) {
                    this.logger.warn("Yarn version has to start with prefix 'v'.");
                }
                if (this.config.getPlatform().isWindows()) {
                    installYarn();
                }
            }
        }
    }

    private boolean yarnIsAlreadyInstalled() {
        try {
            YarnExecutorConfig executorConfig = new InstallYarnExecutorConfig(this.config);
            File nodeFile = executorConfig.getYarnPath();
            if (nodeFile.exists()) {
                final String version =
                    new YarnExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult();

                if (version.equals(this.yarnVersion)) {
                    this.logger.info("Yarn {} is already installed.", version);
                    return true;
                } else {
                    this.logger.info("Yarn {} was installed, but we need version {}", version,
                        this.yarnVersion);
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
            this.logger.info("Installing Yarn version {}", this.yarnVersion);
            String downloadUrl = this.yarnDownloadRoot + this.yarnVersion + "/yarn-"
                + this.yarnVersion.substring(1, this.yarnVersion.length());
            String fileending;
            if (this.config.getPlatform().isWindows()) {
                fileending = ".msi";
            } else {
                fileending = ".tgz";
            }
            downloadUrl += fileending;

            CacheDescriptor cacheDescriptor = new CacheDescriptor("yarn", this.yarnVersion, fileending);

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password);

            File installDirectory = getInstallDirectory();

            // We need to delete the existing yarn directory first so we clean out any old files, and
            // so we can rename the package directory below.
            try {
                if (installDirectory.isDirectory()) {
                    FileUtils.deleteDirectory(installDirectory);
                }
            } catch (IOException e) {
                this.logger.warn("Failed to delete existing Yarn installation.");
            }

            extractFile(archive, installDirectory);

            this.logger.info("Installed Yarn locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Yarn", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the Yarn archive", e);
        }
    }

    private File getInstallDirectory() {
        File installDirectory = new File(this.config.getInstallDirectory(), INSTALL_PATH);
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
