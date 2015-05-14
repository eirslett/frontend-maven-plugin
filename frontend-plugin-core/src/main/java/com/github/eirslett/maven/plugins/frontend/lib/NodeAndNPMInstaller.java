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

public interface NodeAndNPMInstaller {

    String DEFAULT_NODEJS_DOWNLOAD_ROOT = "http://nodejs.org/dist/";
    String DEFAULT_NPM_DOWNLOAD_ROOT = "http://registry.npmjs.org/npm/-/";

    void install(String nodeVersion, String npmVersion, String nodeDownloadRoot, String npmDownloadRoot) throws InstallationException;
}

final class DefaultNodeAndNPMInstaller implements NodeAndNPMInstaller {

    private final Logger logger;
    private final InstallConfig config;
    private final ArchiveExtractor archiveExtractor;
    private final FileDownloader fileDownloader;

    DefaultNodeAndNPMInstaller(InstallConfig config, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    @Override
    public void install(String nodeVersion, String npmVersion, String nodeDownloadRoot, String npmDownloadRoot) throws InstallationException {
        if(nodeDownloadRoot == null || nodeDownloadRoot.isEmpty()){
            nodeDownloadRoot = DEFAULT_NODEJS_DOWNLOAD_ROOT;
        }
        if(npmDownloadRoot == null || npmDownloadRoot.isEmpty()){
            npmDownloadRoot = DEFAULT_NPM_DOWNLOAD_ROOT;
        }
        new NodeAndNPMInstallAction(nodeVersion, npmVersion, nodeDownloadRoot, npmDownloadRoot).install();
    }

    private final class NodeAndNPMInstallAction {
        private static final String VERSION = "version";

        private final String nodeVersion;
        private final String npmVersion;
        private final String nodeDownloadRoot;
        private final String npmDownloadRoot;

        public NodeAndNPMInstallAction(String nodeVersion, String npmVersion, String nodeDownloadRoot, String npmDownloadRoot) {
            this.nodeVersion = nodeVersion;
            this.npmVersion = npmVersion;
            this.nodeDownloadRoot = nodeDownloadRoot;
            this.npmDownloadRoot = npmDownloadRoot;
        }

        public void install() throws InstallationException {
            if(!nodeIsAlreadyInstalled()){
                if(config.getPlatform().isWindows()){
                    installNodeForWindows();
                } else {
                    installNodeDefault();
                }
            }
            if(!npmIsAlreadyInstalled()) {
                installNpm();
            }
        }

        private boolean nodeIsAlreadyInstalled() {
            try {
                NodeExecutorConfig executorConfig = new InstallNodeExecutorConfig(config);
                File nodeFile = executorConfig.getNodePath();
                if(nodeFile.exists()){
                    final String version = new NodeExecutor(executorConfig, Arrays.asList("--version")).executeAndGetResult();

                    if(version.equals(nodeVersion)){
                        logger.info("Node " + version + " is already installed.");
                        return true;
                    } else {
                        logger.info("Node " + version + " was installed, but we need version " + nodeVersion);
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
                final File npmPackageJson = new File(config.getInstallDirectory() + Utils.normalize("/node/npm/package.json"));
                if(npmPackageJson.exists()){
                    HashMap<String,Object> data = new ObjectMapper().readValue(npmPackageJson, HashMap.class);
                    if(data.containsKey(VERSION)){
                        final String foundNpmVersion = data.get(VERSION).toString();
                        logger.info("Found NPM version " + foundNpmVersion);
                        if(foundNpmVersion.equals(npmVersion)) {
                            return true;
                        } else {
                            logger.info("Mismatch between found NPM version and required NPM version");
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
                logger.info("Installing npm version " + npmVersion);
                final String downloadUrl = npmDownloadRoot +"npm-"+npmVersion+".tgz";
                String targetName = config.getInstallDirectory() + File.separator + "npm.tar.gz";
                logger.info("Downloading NPM from " + downloadUrl + " to " + targetName);
                downloadFile(downloadUrl, targetName);

                // We need to delete the existing npm directory first so we clean out any old files, and
                // so we can rename the package directory below.
                File npmDirectory = new File(config.getInstallDirectory(), "./node/npm");
                try {
                    FileUtils.deleteDirectory(npmDirectory);
                } catch (IOException e) {
                    logger.warn("Failed to delete existing NPM installation.");
                }

                logger.info("Extracting NPM files in node/");
                extractFile(targetName, config.getInstallDirectory() +"/node");
                new File(targetName).delete();
                // handles difference between old and new download root (nodejs.org/dist/npm and registry.npmjs.org)
                // see https://github.com/eirslett/frontend-maven-plugin/issues/65#issuecomment-52024254
                File packageDirectory = new File(config.getInstallDirectory(), "./node/package");
                if (packageDirectory.exists() && !npmDirectory.exists()) {
                    if (! packageDirectory.renameTo(npmDirectory)) {
                        logger.warn("Cannot rename NPM directory, making a copy.");
                        FileUtils.copyDirectory(packageDirectory, npmDirectory);
                    }
                }
                logger.info("Installed NPM locally.");
            } catch (DownloadException e) {
                throw new InstallationException("Could not download npm", e);
            } catch (ArchiveExtractionException e) {
                throw new InstallationException("Could not extract the npm archive", e);
            } catch (IOException e) {
                throw new InstallationException("Could not copy npm", e);
            }
        }

        private void installNodeDefault() throws InstallationException {
            String downloadUrl = "";
            try {
                logger.info("Installing node version " + nodeVersion);
                if (!nodeVersion.startsWith("v")) {
                    logger.warn("Node version does not start with naming convention 'v'.");
                }
                final String longNodeFilename = config.getPlatform().getLongNodeFilename(nodeVersion);
                downloadUrl = nodeDownloadRoot + config.getPlatform().getNodeDownloadFilename(nodeVersion);

                final File tmpDirectory = new File(config.getInstallDirectory() + File.separator + "node_tmp");
                logger.info("Creating temporary directory " + tmpDirectory);
                tmpDirectory.mkdirs();

                final String targetName = config.getInstallDirectory() + "/node_tmp/node.tar.gz";
                logger.info("Downloading Node.js from " + downloadUrl + " to " + targetName);
                downloadFile(downloadUrl, targetName);

                logger.info("Extracting Node.js files in node_tmp");
                extractFile(targetName, config.getInstallDirectory() + "/node_tmp");

                // Search for the node binary
                File nodeBinary = new File(config.getInstallDirectory() + "/node_tmp/"+longNodeFilename+"/bin/node");
                if(!nodeBinary.exists()){
                    throw new FileNotFoundException("Could not find the downloaded Node.js binary in "+nodeBinary);
                } else {
                    File destinationDirectory = new File(config.getInstallDirectory() + "/node");
                    destinationDirectory.mkdirs();
                    File destination = new File(destinationDirectory + "/node");
                    logger.info("Moving node binary to " + destination);
                    if(!nodeBinary.renameTo(destination)){
                        throw new InstallationException("Could not install Node: Was not allowed to rename "+nodeBinary+" to "+destination);
                    }

                    if(!destination.setExecutable(true, false)){
                        throw new InstallationException("Cound not install Node: Was not allowed to make "+destination+" executable.");
                    }

                    logger.info("Deleting temporary directory " + tmpDirectory);
                    FileUtils.deleteDirectory(tmpDirectory);

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
                logger.info("Installing node version " + nodeVersion);

                new File(config.getInstallDirectory()+"\\node").mkdirs();

                downloadFile(downloadUrl, config.getInstallDirectory() +"\\node\\node.exe");
                logger.info("Installed node.exe locally.");
            } catch (DownloadException e) {
                throw new InstallationException("Could not download Node.js from: " + downloadUrl, e);
            }
        }

        private void extractFile(String archive, String destinationDirectory) throws ArchiveExtractionException {
            logger.info("Unpacking " + archive + " into " + destinationDirectory);
            archiveExtractor.extract(archive, destinationDirectory);
        }

        private void downloadFile(String downloadUrl, String destination) throws DownloadException {
            fileDownloader.download(downloadUrl, destination);
        }
    }
}
