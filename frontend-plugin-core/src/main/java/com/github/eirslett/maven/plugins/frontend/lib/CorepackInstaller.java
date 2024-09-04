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

public class CorepackInstaller {

    private static final String VERSION = "version";

    public static final String DEFAULT_COREPACK_DOWNLOAD_ROOT = "https://registry.npmjs.org/corepack/-/";

    private static final Object LOCK = new Object();

    private String corepackVersion, corepackDownloadRoot, userName, password;

    private Map<String, String> httpHeaders;
    
    private final Logger logger;

    private final InstallConfig config;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    CorepackInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public CorepackInstaller setNodeVersion(String nodeVersion) {
        return this;
    }

    public CorepackInstaller setCorepackVersion(String corepackVersion) {
        this.corepackVersion = corepackVersion;
        return this;
    }

    public CorepackInstaller setCorepackDownloadRoot(String corepackDownloadRoot) {
        this.corepackDownloadRoot = corepackDownloadRoot;
        return this;
    }

    public CorepackInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public CorepackInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    public CorepackInstaller setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
        return this;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (this.corepackDownloadRoot == null || this.corepackDownloadRoot.isEmpty()) {
                this.corepackDownloadRoot = DEFAULT_COREPACK_DOWNLOAD_ROOT;
            }
            if (!corepackIsAlreadyInstalled()) {
                installCorepack();
            }

            if (this.config.getPlatform().isWindows()) {
                linkExecutableWindows();
            } else {
                linkExecutable();
            }
        }
    }

    private boolean corepackIsAlreadyInstalled() {
        try {
            final File corepackPackageJson = new File(
                this.config.getInstallDirectory() + Utils.normalize("/node/node_modules/corepack/package.json"));
            if (corepackPackageJson.exists()) {
                if ("provided".equals(this.corepackVersion)) {
                    // Since we don't know which version it should be, we must assume that we have
                    // correctly setup the packaged version
                    return true;
                }
                HashMap<String, Object> data = new ObjectMapper().readValue(corepackPackageJson, HashMap.class);
                if (data.containsKey(VERSION)) {
                    final String foundCorepackVersion = data.get(VERSION).toString();
                    if (foundCorepackVersion.equals(this.corepackVersion.replaceFirst("^v", ""))) {
                        this.logger.info("corepack {} is already installed.", foundCorepackVersion);
                        return true;
                    } else {
                        this.logger.info("corepack {} was installed, but we need version {}", foundCorepackVersion,
                            this.corepackVersion);
                        return false;
                    }
                } else {
                    this.logger.info("Could not read corepack version from package.json");
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not read package.json", ex);
        }
    }

    private void installCorepack() throws InstallationException {
        try {
            this.logger.info("Installing corepack version {}", this.corepackVersion);
            String corepackVersionClean = this.corepackVersion.replaceFirst("^v(?=[0-9]+)", "");
            final String downloadUrl = this.corepackDownloadRoot + "corepack-" + corepackVersionClean + ".tgz";

            CacheDescriptor cacheDescriptor = new CacheDescriptor("corepack", corepackVersionClean, "tar.gz");

            File archive = this.config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, this.userName, this.password, this.httpHeaders);

            File installDirectory = getNodeInstallDirectory();
            File nodeModulesDirectory = new File(installDirectory, "node_modules");

            // We need to delete the existing corepack directory first so we clean out any old files, and
            // so we can rename the package directory below.
            File oldDirectory = new File(installDirectory, "corepack");
            File corepackDirectory = new File(nodeModulesDirectory, "corepack");
            try {
                if (oldDirectory.isDirectory()) {
                    FileUtils.deleteDirectory(oldDirectory);
                }
                FileUtils.deleteDirectory(corepackDirectory);
            } catch (IOException e) {
                this.logger.warn("Failed to delete existing corepack installation.");
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
            if (packageDirectory.exists() && !corepackDirectory.exists()) {
                if (!packageDirectory.renameTo(corepackDirectory)) {
                    this.logger.warn("Cannot rename corepack directory, making a copy.");
                    FileUtils.copyDirectory(packageDirectory, corepackDirectory);
                }
            }

            this.logger.info("Installed corepack locally.");

        } catch (DownloadException e) {
            throw new InstallationException("Could not download corepack", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException("Could not extract the corepack archive", e);
        } catch (IOException e) {
            throw new InstallationException("Could not copy corepack", e);
        }
    }

    private void linkExecutable() throws InstallationException{
        File nodeInstallDirectory = getNodeInstallDirectory();
        File corepackExecutable = new File(nodeInstallDirectory, "corepack");

        if (corepackExecutable.exists()) {
            this.logger.info("Existing corepack executable found, skipping linking.");
            return;
        }

        NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(this.config);
        File corepackJsExecutable = executorConfig.getCorepackPath();

        if (!corepackJsExecutable.exists()) {
            throw new InstallationException("Could not link to corepack executable, no corepack installation found.");
        }

        this.logger.info("No corepack executable found, creating symbolic link to {}.", corepackJsExecutable.toPath());

        try {
            Files.createSymbolicLink(corepackExecutable.toPath(), corepackJsExecutable.toPath());
        } catch (IOException e) {
            throw new InstallationException("Could not create symbolic link for corepack executable.", e);
        }
    }

    private void linkExecutableWindows() throws InstallationException{
        File nodeInstallDirectory = getNodeInstallDirectory();
        File corepackExecutable = new File(nodeInstallDirectory, "corepack.cmd");

        if (corepackExecutable.exists()) {
            this.logger.info("Existing corepack executable found, skipping linking.");
            return;
        }

        NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(this.config);
        File corepackJsExecutable = executorConfig.getCorepackPath();

        if (!corepackJsExecutable.exists()) {
            throw new InstallationException("Could not link to corepack executable, no corepack installation found.");
        }

        this.logger.info("No corepack executable found, creating proxy script to {}.", corepackJsExecutable.toPath());

        Path nodePath = executorConfig.getNodePath().toPath();
        Path relativeNodePath = nodeInstallDirectory.toPath().relativize(nodePath);
        Path relativeCorepackPath = nodeInstallDirectory.toPath().relativize(corepackJsExecutable.toPath());

        // Create a script that will proxy any commands passed into it to the corepack executable.
        String scriptContents = new StringBuilder()
                .append(":: Created by frontend-maven-plugin, please don't edit manually.\r\n")
                .append("@ECHO OFF\r\n")
                .append("\r\n")
                .append("SETLOCAL\r\n")
                .append("\r\n")
                .append(String.format("SET \"NODE_EXE=%%~dp0\\%s\"\r\n", relativeNodePath))
                .append(String.format("SET \"COREPACK_CLI_JS=%%~dp0\\%s\"\r\n", relativeCorepackPath))
                .append("\r\n")
                .append("\"%NODE_EXE%\" \"%COREPACK_CLI_JS%\" %*")
                .toString();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(corepackExecutable));
            writer.write(scriptContents);
            writer.close();
        } catch (IOException e) {
            throw new InstallationException("Could not create proxy script for corepack executable.", e);
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
