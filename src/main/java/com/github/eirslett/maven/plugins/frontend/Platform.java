package com.github.eirslett.maven.plugins.frontend;

public class Platform {
    private final OS os;
    private final Architecture architecture;

    public Platform(OS os, Architecture architecture) {
        this.os = os;
        this.architecture = architecture;
    }

    public static Platform guess(){
        OS os = OS.guess();
        Architecture architecture = Architecture.guess();
        return new Platform(os,architecture);
    }

    public String getCodename(){
        return os.getCodename();
    }

    public boolean isWindows(){
        return os == OS.Windows;
    }

    public String getLongNodeFilename(String nodeVersion) {
        if(isWindows()){
            return "node.exe";
        } else {
            return "node-" + nodeVersion + "-" + getCodename() + "-" + architecture.toString();
        }
    }

    public String getNodeDownloadFilename(String nodeVersion) {
        if(isWindows()) {
            if(architecture == Architecture.x64){
                return nodeVersion+"/x64/node.exe";
            } else {
                return nodeVersion + "/node.exe";
            }
        } else {
            return nodeVersion + "/" + getLongNodeFilename(nodeVersion) + ".tar.gz";
        }
    }
}
