package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.plugins.annotations.Parameter;

public class PlatformTarget {

    @Parameter(property = "os", required = true)
    private String os;

    @Parameter(property = "architecture", required = true)
    private String architecture;


    public String getOs() {
        return os;
    }


    public void setOs(String os) {
        this.os = os;
    }


    public String getArchitecture() {
        return architecture;
    }


    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

}