package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

public class BunInstaller {

    public static final String INSTALL_PATH = "/bun";

    public static final String DEFAULT_BUN_DOWNLOAD_ROOT =
            "https://github.com/oven-sh/bun/releases/download/";
    private static final Object LOCK = new Object();

    private String bunVersion, userName, password;

    private Map<String, String> httpHeaders;
    
    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    BunInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public BunInstaller setBunVersion(String bunVersion) {
        this.bunVersion = bunVersion;
        return this;
    }

    public BunInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public BunInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    public BunInstaller setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }
    
    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (!bunIsAlreadyInstalled()) {
                if (!this.bunVersion.startsWith("v")) {
                    this.logger.warn("Bun version does not start with naming convention 'v'.");
                }
                installBunDefault();
            }
        }
    }

    private boolean bunIsAlreadyInstalled() {
        try {
            BunExecutorConfig executorConfig = new InstallBunExecutorConfig(config);
            File bunFile = executorConfig.getBunPath();
            if (bunFile.exists()) {
                final String version =
                        new BunExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult(logger);

                if (version.equals(this.bunVersion.replaceFirst("^v", ""))) {
                    this.logger.info("Bun {} is already installed.", version);
                    return true;
                } else {
                    this.logger.info("Bun {} was installed, but we need version {}", version,
                            this.bunVersion);
                    return false;
                }
            } else {
                return false;
            }
        } catch (ProcessExecutionException e) {
            this.logger.warn("Unable to determine current bun version: {}", e.getMessage());
            return false;
        }
    }

    private void installBunDefault() throws InstallationException {
        try {

            logger.info("Installing Bun version {}", bunVersion);

            String downloadUrl = createDownloadUrl();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("bun", this.bunVersion,
                    "zip");

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password, this.httpHeaders);

            File installDirectory = getInstallDirectory();

            // We need to delete the existing bun directory first so we clean out any old files, and
            // so we can rename the package directory below.
            File bunExtractDirectory = new File(installDirectory, createBunTargetArchitecturePath());
            try {
                if (bunExtractDirectory.isDirectory()) {
                    FileUtils.deleteDirectory(bunExtractDirectory);
                }
            } catch (IOException e) {
                logger.warn("Failed to delete existing Bun installation.");
            }

            try {
                extractFile(archive, installDirectory);
            } catch (ArchiveExtractionException e) {
                if (e.getCause() instanceof EOFException) {
                    this.logger.error("The archive file {} is corrupted and will be deleted. "
                            + "Please try the build again.", archive.getPath());
                    archive.delete();
                }
                throw e;
            }

            // Search for the bun binary
            String bunExecutable = this.config.getPlatform().isWindows()  ? "bun.exe" : "bun";
            File bunBinary =
                    new File(installDirectory, File.separator + createBunTargetArchitecturePath() + File.separator + bunExecutable);
            if (!bunBinary.exists()) {
                throw new FileNotFoundException(
                        "Could not find the downloaded bun binary in " + bunBinary);
            } else {
                File destinationDirectory = new File(getInstallDirectory(), BunInstaller.INSTALL_PATH);
                if (!destinationDirectory.exists()) {
                    this.logger.info("Creating destination directory {}", destinationDirectory);
                    destinationDirectory.mkdirs();
                }

                File destination = new File(destinationDirectory, bunExecutable);
                this.logger.info("Copying bun binary from {} to {}", bunBinary, destination);
                if (destination.exists() && !destination.delete()) {
                    throw new InstallationException("Could not install Bun: Was not allowed to delete " + destination);
                }
                try {
                    Files.move(bunBinary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new InstallationException("Could not install Bun: Was not allowed to rename "
                            + bunBinary + " to " + destination);
                }

                if (!destination.setExecutable(true, false)) {
                    throw new InstallationException(
                            "Could not install Bun: Was not allowed to make " + destination + " executable.");
                }
                FileUtils.deleteDirectory(bunExtractDirectory);

                this.logger.info("Installed bun locally.");
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install bun", e);
        } catch (DownloadException e) {
            throw new InstallationException("Could not download bun", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the bun archive", e);
        }
    }

    private String createDownloadUrl() {
        String downloadUrl = String.format("%sbun-%s", DEFAULT_BUN_DOWNLOAD_ROOT, bunVersion);
        String extension = "zip";
        String fileending = String.format("%s.%s", createBunTargetArchitecturePath(), extension);

        downloadUrl += fileending;
        return downloadUrl;
    }

    private String createBunTargetArchitecturePath() {
        OS os = OS.guess();
        Architecture architecture = Architecture.guess();
        String destOs = os.equals(OS.Linux) ? "linux" : os.equals(OS.Mac) ? "darwin" : os.equals(OS.Windows) ? "windows" : null;
        String destArc = architecture.equals(Architecture.x64) ? "x64" : architecture.equals(
                Architecture.arm64) ? "aarch64" : null;
        return String.format("%s-%s-%s", INSTALL_PATH, destOs, destArc);
    }

    private File getInstallDirectory() {
        File installDirectory = new File(this.config.getInstallDirectory(), "/");
        if (!installDirectory.exists()) {
            this.logger.info("Creating install directory {}", installDirectory);
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
