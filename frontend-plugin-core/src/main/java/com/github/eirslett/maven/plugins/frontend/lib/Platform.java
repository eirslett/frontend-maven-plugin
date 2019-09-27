package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum Architecture { x86, x64, ppc64le, s390x, arm64, armv7l;
    public static Architecture guess(){
        String arch = System.getProperty("os.arch");
        String version = System.getProperty("os.version");

        if (arch.equals("ppc64le")) {
            return ppc64le;
        } else if (arch.equals("aarch64")) {
            return arm64;
        } else if (arch.equals("s390x")) {
                return s390x;		
        } else if (arch.equals("arm") && version.contains("v7")) {
                return armv7l;		
        } else {
            return arch.contains("64") ? x64 : x86;
        }
    }
}

enum OS { Windows, Mac, Linux, SunOS;

    public static OS guess() {
        final String osName = System.getProperty("os.name");
        return  osName.contains("Windows") ? OS.Windows :
                osName.contains("Mac") ? OS.Mac :
                        osName.contains("SunOS") ? OS.SunOS :
                                OS.Linux;
    }

    public String getArchiveExtension(){
        if(this == OS.Windows){
          return "zip";
        } else {
          return "tar.gz";
        }
    }

    public String getCodename(){
        if(this == OS.Mac){
            return "darwin";
        } else if(this == OS.Windows){
            return "win";
        } else if(this == OS.SunOS){
            return "sunos";
        } else {
            return "linux";
        }
    }
}

class Platform {
    private final OS os;
    private final Architecture architecture;
    private final boolean experimentalArchitecture;
    private final String nodeClassifierSuffix;

    public Platform(OS os, Architecture architecture) {
        this.os = os;
        this.architecture = architecture;
        
        // guess experimental architecture (eg. Alpine Linux) using "ldd" command
        String stdout = "";
        Logger logger = LoggerFactory.getLogger(getClass());
        try {
            ProcessExecutor executor = new ProcessExecutor(new File("/"), Collections.emptyList(),
                    Utils.prepend("ldd", Arrays.asList("/bin/true")),
                    this, Collections.emptyMap());
            stdout = executor.executeAndGetResult(logger);
        } catch (ProcessExecutionException e) {
            logger.warn(e.getMessage());
        }
        if (stdout.indexOf("ld-musl-x86_64") > -1) {
            this.experimentalArchitecture = true;
            this.nodeClassifierSuffix = "-musl";
        }
        else {
            this.experimentalArchitecture = false;
            this.nodeClassifierSuffix = "";
        }
    }

    public static Platform guess(){
        OS os = OS.guess();
        Architecture architecture = Architecture.guess();
        return new Platform(os,architecture);
    }

    public String getArchiveExtension(){
        return os.getArchiveExtension();
    }

    public String getCodename(){
        return os.getCodename();
    }

    public boolean isWindows(){
        return os == OS.Windows;
    }

    public boolean isMac(){
        return os == OS.Mac;
    }
    
    public boolean isExperimentalArchitecture() {
        return experimentalArchitecture;
    }

    public String getLongNodeFilename(String nodeVersion, boolean archiveOnWindows) {
        if(isWindows() && !archiveOnWindows){
            return "node.exe";
        } else {
            return "node-" + nodeVersion + "-" + this.getNodeClassifier();
        }
    }

    public String getNodeDownloadFilename(String nodeVersion, boolean archiveOnWindows) {
        if(isWindows() && !archiveOnWindows) {
            if(architecture == Architecture.x64){
                if (nodeVersion.startsWith("v0.")) {
                    return nodeVersion+"/x64/node.exe";
                } else {
                    return nodeVersion+"/win-x64/node.exe";
                }
            } else {
                if (nodeVersion.startsWith("v0.")) {
                	return nodeVersion + "/node.exe";
                } else {
                    return nodeVersion+"/win-x86/node.exe";
                }
            }
        } else {
            return nodeVersion + "/" + getLongNodeFilename(nodeVersion, archiveOnWindows) + "." + os.getArchiveExtension();
        }
    }

    public String getNodeClassifier() {
        return this.getCodename() + "-" + this.architecture.name() + this.nodeClassifierSuffix;
    }
}
