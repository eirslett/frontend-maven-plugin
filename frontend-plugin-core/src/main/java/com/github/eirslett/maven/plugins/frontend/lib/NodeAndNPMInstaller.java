package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class NodeAndNPMInstaller {

    private static final String INSTALL_PATH = "node";
    private static final String VERSION = "version";

    public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";
    public static final String DEFAULT_NPM_DOWNLOAD_ROOT = "http://registry.npmjs.org/npm/-/";

    private static final Object LOCK = new Object();

    private String nodeVersion, npmVersion, nodeDownloadRoot, npmDownloadRoot, userName, password;

    private final Logger logger;
    private final InstallConfig config;
    private final ArchiveExtractor archiveExtractor;
    private final FileDownloader fileDownloader;

    NodeAndNPMInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    public NodeAndNPMInstaller setNodeVersion (String nodeVersion) {
        this.nodeVersion = nodeVersion;
        return this;
    }

    public NodeAndNPMInstaller setNodeDownloadRoot (String nodeDownloadRoot) {
        this.nodeDownloadRoot = nodeDownloadRoot;
        return this;
    }

    public NodeAndNPMInstaller setNpmVersion (String npmVersion) {
        this.npmVersion = npmVersion;
        return this;
    }

    public NodeAndNPMInstaller setNpmDownloadRoot (String npmDownloadRoot) {
        this.npmDownloadRoot = npmDownloadRoot;
        return this;
    }

    public NodeAndNPMInstaller setUserName (String userName) {
        this.userName = userName;
        return this;
    }

    public NodeAndNPMInstaller setPassword (String password) {
        this.password = password;
        return this;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if(nodeDownloadRoot == null || nodeDownloadRoot.isEmpty()){
                nodeDownloadRoot = DEFAULT_NODEJS_DOWNLOAD_ROOT;
            }
            if(npmDownloadRoot == null || npmDownloadRoot.isEmpty()){
                npmDownloadRoot = DEFAULT_NPM_DOWNLOAD_ROOT;
            }
            if (!nodeIsAlreadyInstalled()) {
                logger.info("Installing node version {}", nodeVersion);
                if (!nodeVersion.startsWith("v")) {
                    logger.warn("Node version does not start with naming convention 'v'.");
                }
                if (config.getPlatform().isWindows()) {
                    installNodeForWindows();
                } else {
                    installNodeDefault();
                }
            }
            if (!npmIsAlreadyInstalled()) {
                installNpm();
            }
        }
    }

    private boolean nodeIsAlreadyInstalled() {
        try {
            NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(config);
            File nodeFile = executorConfig.getNodePath();
            if(nodeFile.exists()){
                final String version = new NodeExecutor(executorConfig, Arrays.asList("--version"), null).executeAndGetResult();

                if(version.equals(nodeVersion)){
                    logger.info("Node {} is already installed.", version);
                    return true;
                } else {
                    logger.info("Node {} was installed, but we need version {}", version, nodeVersion);
                    return false;
                }
            } else {
                return false;
            }
        } catch (ProcessExecutionException e) {
            return false;
        }
    }

    private boolean npmIsAlreadyInstalled(){
        try {
            final File npmPackageJson = new File(config.getInstallDirectory() + Utils.normalize("/node/node_modules/npm/package.json"));
            if(npmPackageJson.exists()){
                HashMap<String,Object> data = new ObjectMapper().readValue(npmPackageJson, HashMap.class);
                if(data.containsKey(VERSION)){
                    final String foundNpmVersion = data.get(VERSION).toString();
                    if(foundNpmVersion.equals(npmVersion)) {
                        logger.info("NPM {} is already installed.", foundNpmVersion);
                        return true;
                    } else {
                        logger.info("NPM {} was installed, but we need version {}", foundNpmVersion, npmVersion);
                        return false;
                    }
                } else {
                    logger.info("Could not read NPM version from package.json");
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException ex){
            throw new RuntimeException("Could not read package.json", ex);
        }
    }

    private void installNpm() throws InstallationException {
        try {
            logger.info("Installing npm version {}", npmVersion);
            final String downloadUrl = npmDownloadRoot +"npm-"+npmVersion+".tgz";

            CacheDescriptor cacheDescriptor = new CacheDescriptor("npm", npmVersion, "tar.gz");

            File archive = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, userName, password);

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
                logger.warn("Failed to delete existing NPM installation.");
            }

            extractFile(archive, nodeModulesDirectory);

            // handles difference between old and new download root (nodejs.org/dist/npm and registry.npmjs.org)
            // see https://github.com/eirslett/frontend-maven-plugin/issues/65#issuecomment-52024254
            File packageDirectory = new File(nodeModulesDirectory, "package");
            if (packageDirectory.exists() && !npmDirectory.exists()) {
                if (! packageDirectory.renameTo(npmDirectory)) {
                    logger.warn("Cannot rename NPM directory, making a copy.");
                    FileUtils.copyDirectory(packageDirectory, npmDirectory);
                }
            }

            // create a copy of the npm scripts next to the node executable
            for (String script : Arrays.asList("npm", "npm.cmd")) {
                File scriptFile = new File(npmDirectory, "bin"+File.separator+script);
                if (scriptFile.exists()) {
                    File copy = new File(installDirectory, script);
                    FileUtils.copyFile(scriptFile, copy);
                    copy.setExecutable(true);
                }
            }

            logger.info("Installed npm locally.");
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
            final String longNodeFilename = config.getPlatform().getLongNodeFilename(nodeVersion);
            String downloadUrl = nodeDownloadRoot + config.getPlatform().getNodeDownloadFilename(nodeVersion);
            String classifier = config.getPlatform().getNodeClassifier();

            File tmpDirectory = getTempDirectory();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("node", nodeVersion, classifier, "tar.gz");

            File archive = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, archive, userName, password);

            extractFile(archive, tmpDirectory);

            // Search for the node binary
            File nodeBinary = new File(tmpDirectory,longNodeFilename + File.separator + "bin" + File.separator + "node");
            if(!nodeBinary.exists()){
                throw new FileNotFoundException("Could not find the downloaded Node.js binary in "+nodeBinary);
            } else {
                File destinationDirectory = getInstallDirectory();

                File destination = new File(destinationDirectory, "node");
                logger.info("Copying node binary from {} to {}", nodeBinary, destination);
                if(!nodeBinary.renameTo(destination)){
                    throw new InstallationException("Could not install Node: Was not allowed to rename "+nodeBinary+" to "+destination);
                }

                if(!destination.setExecutable(true, false)){
                  throw new InstallationException("Could not install Node: Was not allowed to make "+destination+" executable.");
                }

                deleteTempDirectory(tmpDirectory);

                logger.info("Installed node locally.");
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
        final String downloadUrl = nodeDownloadRoot + config.getPlatform().getNodeDownloadFilename(nodeVersion);
        try {
            File destinationDirectory = getInstallDirectory();

            File destination = new File(destinationDirectory, "node.exe");

            String classifier = config.getPlatform().getNodeClassifier();

            CacheDescriptor cacheDescriptor = new CacheDescriptor("node", nodeVersion, classifier, "exe");

            File binary = config.getCacheResolver().resolve(cacheDescriptor);

            downloadFileIfMissing(downloadUrl, binary, userName, password);

            logger.info("Copying node binary from {} to {}", binary, destination);
            FileUtils.copyFile(binary, destination);

            logger.info("Installed node locally.");
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Node.js from: " + downloadUrl, e);
        } catch (IOException e) {
            throw new InstallationException("Could not install Node.js", e);
        }
    }

    private File getTempDirectory() {
        File tmpDirectory = new File(getInstallDirectory(), "tmp");
        if (!tmpDirectory.exists()) {
            logger.debug("Creating temporary directory {}", tmpDirectory);
            tmpDirectory.mkdirs();
        }
        return tmpDirectory;
    }

    private File getInstallDirectory() {
        File installDirectory = new File(config.getInstallDirectory(), INSTALL_PATH);
        if (!installDirectory.exists()) {
            logger.debug("Creating install directory {}", installDirectory);
            installDirectory.mkdirs();
        }
        return installDirectory;
    }

    private void deleteTempDirectory(File tmpDirectory) throws IOException {
        if (tmpDirectory != null && tmpDirectory.exists()) {
            logger.debug("Deleting temporary directory {}", tmpDirectory);
            FileUtils.deleteDirectory(tmpDirectory);
        }
    }

    private void extractFile(File archive, File destinationDirectory) throws ArchiveExtractionException {
        logger.info("Unpacking {} into {}", archive, destinationDirectory);
        archiveExtractor.extract(archive.getPath(), destinationDirectory.getPath());
    }

    private void downloadFileIfMissing(String downloadUrl, File destination, String userName, String password) throws DownloadException {
        if (!destination.exists()) {
            downloadFile(downloadUrl, destination, userName, password);
        }
    }

    private void downloadFile(String downloadUrl, File destination, String userName, String password) throws DownloadException {
        logger.info("Downloading {} to {}", downloadUrl, destination);
        fileDownloader.download(downloadUrl, destination.getPath(), userName, password);
    }
}
