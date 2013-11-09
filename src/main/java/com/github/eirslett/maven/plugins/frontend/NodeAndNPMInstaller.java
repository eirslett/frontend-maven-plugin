package com.github.eirslett.maven.plugins.frontend;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.FileUtils;

final class NodeAndNPMInstaller {
    private final String nodeVersion;
    private final String npmVersion;
    private final File workingDirectory;
    private final Log log;
    private final Architecture architecture;
    private static final String DOWNLOAD_ROOT = "http://nodejs.org/dist/",
            VERSION = "version";

    public NodeAndNPMInstaller(String nodeVersion, String npmVersion, File workingDirectory, Log log) {
        this.workingDirectory = workingDirectory;
        this.log = log;
        this.architecture = Architecture.guess();
        this.nodeVersion = nodeVersion;
        this.npmVersion = npmVersion;
    }

    public void install() throws MojoFailureException {
        if(!nodeIsAlreadyInstalled()){
            if(OS.isWindows()){
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
        if(OS.isWindows()){
            return nodeIsAlreadyInstalledOnWindows();
        } else {
            return nodeIsAlreadyInstalledDefault();
        }
    }

    private boolean nodeIsAlreadyInstalledOnWindows() {
        final File nodeFile = new File(workingDirectory + "\\node\\node.exe");
        if(nodeFile.exists()){
            final String version = new NodeExecutor(workingDirectory, Arrays.asList("--version")).executeAndGetResult();
            if(version.equals(nodeVersion)){
                log.info("Node "+version+" is already installed.");
                return true;
            } else {
                log.info("Node "+version+" was installed, but we need version "+nodeVersion);
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean nodeIsAlreadyInstalledDefault() {
        final File nodeFile = new File(workingDirectory + "/node/node");
        if(nodeFile.exists()){
            final String version = new NodeExecutor(workingDirectory, Arrays.asList("--version")).executeAndGetResult();
            if(version.equals(nodeVersion)){
                log.info("Node "+version+" is already installed.");
                return true;
            } else {
                log.info("Node "+version+" was installed, but we need version "+nodeVersion);
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean npmIsAlreadyInstalled(){
        Scanner scanner = null;
        try {
            final File npmPackageJson = new File(workingDirectory + "/node/npm/package.json".replace("/", File.separator));
            if(npmPackageJson.exists()){
                HashMap<String,Object> data = new ObjectMapper().readValue(npmPackageJson, HashMap.class);
                if(data.containsKey(VERSION)){
                    final String foundNpmVersion = data.get(VERSION).toString();
                    log.info("Found NPM version "+foundNpmVersion);
                    if(foundNpmVersion.equals(npmVersion)) {
                        return true;
                    } else {
                        log.info("Mismatch between found NPM version and required NPM version");
                        return false;
                    }
                } else {
                    log.info("Could nog read NPM version from package.json");
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException ex){
            throw new RuntimeException("Could not read package.json", ex);
        } finally {
            if(scanner != null)
                scanner.close();
        }
    }

    private void installNpm() throws MojoFailureException {
        String downloadUrl = "";
        try {
            log.info("Installing NPM version " + npmVersion);
            downloadUrl = DOWNLOAD_ROOT+"npm/npm-"+npmVersion+".tgz";
            String targetName = workingDirectory + File.separator + "npm.tar.gz";
            downloadFile(downloadUrl, targetName);

            final File archive = new File(targetName);
            extractFile(new File(workingDirectory +"/node"), archive);

            new File(targetName).delete();
            log.info("Installed NPM locally.");
        } catch (IOException e) {
            throw new MojoFailureException("Could not download NPM from "+downloadUrl, e);
        }
    }

    private void extractFile(File destinationDirectory, File archive){
        final TarGZipUnArchiver unarchiver = new TarGZipUnArchiver(archive);
        unarchiver.enableLogging(new ConsoleLogger(Logger.LEVEL_INFO, "console" ));
        unarchiver.setDestDirectory(destinationDirectory);
        unarchiver.extract();
    }

    private void installNodeDefault() throws MojoFailureException {
        String downloadUrl = "";
        try {
            log.info("Installing node version " + nodeVersion);
            final String osName = getOsCodeName();
            final String longNodeFilename = "node-" + nodeVersion + "-" + osName + "-" + architecture.toString();
            downloadUrl = DOWNLOAD_ROOT + nodeVersion + "/" + longNodeFilename + ".tar.gz";

            final File tmpDirectory = new File(workingDirectory + File.separator + "node_tmp");
            log.info("Creating temporary directory "+tmpDirectory);
            tmpDirectory.mkdirs();

            final String targetName = workingDirectory + "/node_tmp/node.tar.gz";
            log.info("Downloading Node.js from "+downloadUrl+" to "+targetName);
            downloadFile(downloadUrl, targetName);

            final File archive = new File(targetName);
            log.info("Extracting Node.js files in node_tmp");
            extractFile(new File(workingDirectory + "/node_tmp"), archive);

            // Search for the node binary
            File nodeBinary = new File(workingDirectory + "/node_tmp/"+longNodeFilename+"/bin/node");
            if(!nodeBinary.exists()){
                throw new FileNotFoundException("Could not find the downloaded Node.js binary in "+nodeBinary);
            } else {
                File destinationDirectory = new File(workingDirectory + "/node");
                destinationDirectory.mkdirs();
                File destination = new File(workingDirectory + "/node/node");
                log.info("Moving node binary to "+destination);
                if(!nodeBinary.renameTo(destination)){
                    throw new MojoFailureException("Could not install Node: Was not allowed to rename "+nodeBinary+" to "+destination);
                }

                if(!destination.setExecutable(true, false)){
                    throw new MojoFailureException("Cound not install Node: Was not allowed to make "+destination+" executable.");
                }


                log.info("Deleting temporary directory "+tmpDirectory);
                FileUtils.deleteDirectory(tmpDirectory);

                log.info("Installed node locally.");
            }
        } catch (IOException e) {
            throw new MojoFailureException("Could not download Node.js from "+downloadUrl, e);
        }
    }

    private void installNodeForWindows() throws MojoFailureException {
        try {
            log.info("Installing node version " + nodeVersion);
            final String downloadUrl;
            if(architecture == Architecture.x64){
                downloadUrl = DOWNLOAD_ROOT+nodeVersion+"/x64/node.exe";
            } else {
                downloadUrl = DOWNLOAD_ROOT+nodeVersion+"/node.exe";
            }

            new File(workingDirectory+"\\node").mkdirs();

            downloadFile(downloadUrl, workingDirectory +"\\node\\node.exe");
            log.info("Installed node.exe locally.");
        } catch (MalformedURLException e){
            throw new MojoFailureException("The Node.js download link was invalid", e);
        } catch (IOException e){
            throw new MojoFailureException("Could not download Node.js", e);
        }
    }

    private void downloadFile(String downloadUrl, String destination) throws IOException {
        new File(FileUtils.dirname(destination)).mkdirs();
        URL link = new URL(downloadUrl);
        ReadableByteChannel rbc = Channels.newChannel(link.openStream());
        FileOutputStream fos = new FileOutputStream(destination);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
    }

    private String getOsCodeName() {
        OS os = OS.guess();
        if(os == OS.Mac){
            return "darwin";
        } else if(os == OS.SunOS){
            return "sunos";
        } else {
            return "linux";
        }
    }
}
