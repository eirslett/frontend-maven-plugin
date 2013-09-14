package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.plexus.util.FileUtils;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Scanner;

public final class NodeAndNPMInstaller {
    private final String nodeVersion;
    private final String npmVersion;
    private final String targetDir;
    private final Log log;
    private final OS os;
    private final Architecture architecture;
    private static final String DOWNLOAD_ROOT = "http://nodejs.org/dist/";


    public NodeAndNPMInstaller(String nodeVersion, String npmVersion, String targetDir, Log log){
        this(nodeVersion, npmVersion, targetDir, log, OS.guess(), Architecture.guess());
    }
    public NodeAndNPMInstaller(String nodeVersion, String npmVersion, String targetDir, Log log, OS os, Architecture architecture) {
        this.os = os;
        this.targetDir = targetDir;
        this.log = log;
        this.architecture = architecture;
        this.nodeVersion = nodeVersion;
        this.npmVersion = npmVersion;
    }

    public void install() throws MojoFailureException {
        if(!npmIsAlreadyInstalled())
            installNpm();

        if(!nodeIsAlreadyInstalled()){
            if(os == OS.Windows){
                installNodeForWindows();
            } else {
                installNodeDefault();
            }
        }
    }

    private boolean nodeIsAlreadyInstalled() {
        if(os == OS.Windows){
            return nodeIsAlreadyInstalledOnWindows();
        } else {
            return false;
        }
    }

    private boolean nodeIsAlreadyInstalledOnWindows() {
        final File nodeFile = new File(targetDir + "\\node\\node.exe");
        if(nodeFile.exists()){
            final String version = new NodeExecutor(new File(targetDir), log).executeWithResult(" --version");
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
            final File npmPackageJson = new File(targetDir + "/node/npm/package.json".replace("/", File.separator));
            if(npmPackageJson.exists()){
                HashMap<String,Object> data = new ObjectMapper().readValue(npmPackageJson, HashMap.class);
                if(data.containsKey("version")){
                    final String foundNpmVersion = data.get("version").toString();
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
            String targetName = targetDir + File.separator + "npm.tar.gz";
            downloadFile(downloadUrl, targetName);

            final File archive = new File(targetName);
            final Archiver archiver = ArchiverFactory.createArchiver(archive);
            archiver.extract(archive, new File(targetDir+"/node"));

            new File(targetName).delete();
            log.info("Installed NPM locally.");
        } catch (IOException e) {
            throw new MojoFailureException("Could not download NPM from "+downloadUrl, e);
        }
    }

    private void installNodeDefault() throws MojoFailureException {
        String downloadUrl = "";
        try {
            log.info("Installing node version " + nodeVersion);
            final String osName = getOsName();
            final String architecture = this.architecture.toString();
            downloadUrl = DOWNLOAD_ROOT+nodeVersion+"/node-"+nodeVersion+"-"+osName+"-"+architecture+".tar.gz";
            final String targetName = targetDir + "/node/node.tar.gz";
            downloadFile(downloadUrl, targetName);

            final File archive = new File(targetName);
            final Archiver archiver = ArchiverFactory.createArchiver(archive);
            archiver.extract(archive, new File(targetDir));
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

            downloadFile(downloadUrl, targetDir+"/node/node.exe");
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

    private String getOsName() {
        if(os == OS.Mac){
            return "darwin";
        } else if(os == OS.SunOS){
            return "sunos";
        } else {
            return "linux";
        }
    }
}
