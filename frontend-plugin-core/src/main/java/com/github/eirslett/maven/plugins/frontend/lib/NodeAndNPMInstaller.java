package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.eirslett.maven.plugins.frontend.lib.Utils.normalize;

public interface NodeAndNPMInstaller {
    void install(String nodeVersion, String npmVersion) throws InstallationException;
    void install(String nodeVersion, String npmVersion, String downloadRoot) throws InstallationException;
}

final class DefaultNodeAndNPMInstaller implements NodeAndNPMInstaller {
    private static final String DEFAULT_DOWNLOAD_ROOT = "http://nodejs.org/dist/";

    private final File workingDirectory;
    private final Logger logger;
    private final Platform platform;
    private final ArchiveExtractor archiveExtractor;
    private final FileDownloader fileDownloader;

    DefaultNodeAndNPMInstaller(File workingDirectory, Platform platform, ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.workingDirectory = workingDirectory;
        this.logger = LoggerFactory.getLogger(getClass());
        this.platform = platform;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    @Override
    public void install(String nodeVersion, String npmVersion) throws InstallationException {
        new NodeAndNPMInstallAction(nodeVersion, npmVersion, null).install();
    }

    @Override
    public void install(String nodeVersion, String npmVersion, String downloadRoot) throws InstallationException {
        if(downloadRoot == null || downloadRoot.isEmpty()){
            downloadRoot = DEFAULT_DOWNLOAD_ROOT;
        }
        new NodeAndNPMInstallAction(nodeVersion, npmVersion, downloadRoot).install();
    }

    private final class NodeAndNPMInstallAction {
        private static final String VERSION = "version";

        private final String nodeVersion;
        private final String npmVersion;
        private final String downloadRoot;

        public NodeAndNPMInstallAction(String nodeVersion, String npmVersion, String downloadRoot) {
            this.nodeVersion = nodeVersion;
            this.npmVersion = npmVersion;
            this.downloadRoot = downloadRoot;
        }

        public void install() throws InstallationException {
            if(!nodeIsAlreadyInstalled()){
                if(platform.isWindows()){
                    installNodeForWindows();
                } else {
                    installNodeDefault();
                }
            }
            if(!npmIsAlreadyInstalled()) {
                installNpm();
            }
            if(!platform.isWindows() && !hasShortcutScripts()){
                createShortcutScripts();
            }
        }

        private boolean nodeIsAlreadyInstalled() {
            if(platform.isWindows()){
                return nodeIsAlreadyInstalledOnWindows();
            } else {
                return nodeIsAlreadyInstalledDefault();
            }
        }

        private boolean nodeIsAlreadyInstalledOnWindows(){
            return nodeIsAlreadyInstalled("\\node\\node.exe");
        }

        private boolean nodeIsAlreadyInstalledDefault(){
            return nodeIsAlreadyInstalled("/node/node");
        }

        private boolean nodeIsAlreadyInstalled(String nodeExeutable) {
            try {
                final File nodeFile = new File(workingDirectory + nodeExeutable);
                if(nodeFile.exists()){
                    final String version = new NodeExecutor(workingDirectory, Arrays.asList("--version"), platform).executeAndGetResult();

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
                final File npmPackageJson = new File(workingDirectory + Utils.normalize("/node/npm/package.json"));
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
                final String downloadUrl = downloadRoot +"npm/npm-"+npmVersion+".tgz";
                String targetName = workingDirectory + File.separator + "npm.tar.gz";
                downloadFile(downloadUrl, targetName);
                extractFile(targetName, workingDirectory +"/node");
                new File(targetName).delete();
                logger.info("Installed NPM locally.");
            } catch (DownloadException e) {
                throw new InstallationException("Could not download npm", e);
            } catch (ArchiveExtractionException e) {
                throw new InstallationException("Could not extract the npm archive", e);
            }
        }

        private void installNodeDefault() throws InstallationException {
            String downloadUrl = "";
            try {
                logger.info("Installing node version " + nodeVersion);
                if (!nodeVersion.startsWith("v")) {
                    logger.warn("Node version does not start with naming convention 'v'.");
                }
                final String longNodeFilename = platform.getLongNodeFilename(nodeVersion);
                downloadUrl = downloadRoot + platform.getNodeDownloadFilename(nodeVersion);

                final File tmpDirectory = new File(workingDirectory + File.separator + "node_tmp");
                logger.info("Creating temporary directory " + tmpDirectory);
                tmpDirectory.mkdirs();

                final String targetName = workingDirectory + "/node_tmp/node.tar.gz";
                logger.info("Downloading Node.js from " + downloadUrl + " to " + targetName);
                downloadFile(downloadUrl, targetName);

                logger.info("Extracting Node.js files in node_tmp");
                extractFile(targetName, workingDirectory + "/node_tmp");

                // Search for the node binary
                File nodeBinary = new File(workingDirectory + "/node_tmp/"+longNodeFilename+"/bin/node");
                if(!nodeBinary.exists()){
                    throw new FileNotFoundException("Could not find the downloaded Node.js binary in "+nodeBinary);
                } else {
                    File destinationDirectory = new File(workingDirectory + "/node");
                    destinationDirectory.mkdirs();
                    File destination = new File(workingDirectory + "/node/node");
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
            final String downloadUrl = downloadRoot + platform.getNodeDownloadFilename(nodeVersion);
            try {
                logger.info("Installing node version " + nodeVersion);                

                new File(workingDirectory+"\\node").mkdirs();

                downloadFile(downloadUrl, workingDirectory +"\\node\\node.exe");
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

    private boolean hasShortcutScripts() {
        return new File(workingDirectory+normalize("/node/with_new_path.sh")).exists();
    }

    private void createShortcutScripts() throws InstallationException {
        try {
            File script = new File(workingDirectory+normalize("/node/with_new_path.sh"));
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(script)));
            pw.println("#!/bin/sh");
            if(platform.isMac()) {
                // issue 23: readlink not working on mac osx
                pw.println("export PATH=\"$(dirname $(python -c 'import os,sys;print os.path.realpath(sys.argv[1])' $0)):$PATH\"");
            }
            else {
                pw.println("export PATH=\"$(dirname $(readlink -f $0)):$PATH\"");
            }
            pw.println("\"$@\"");
            pw.close();
            logger.info("Created npm script "+script);
        } catch (IOException e) {
            throw new InstallationException("Could not create path script", e);
        }
    }
}
