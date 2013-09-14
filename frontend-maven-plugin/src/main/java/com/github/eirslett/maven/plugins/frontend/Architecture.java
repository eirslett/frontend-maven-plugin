package com.github.eirslett.maven.plugins.frontend;

public enum Architecture { x86, x64;
    public static Architecture guess(){
        return System.getProperty("os.arch").contains("64") ? x64 : x86;
    }
}
