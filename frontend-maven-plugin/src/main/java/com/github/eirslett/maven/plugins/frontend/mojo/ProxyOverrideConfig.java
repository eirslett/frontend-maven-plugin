package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.plugins.annotations.Parameter;

public class ProxyOverrideConfig {
    @Parameter(required = true)
    private String host;

    @Parameter
    private Integer port = 8080;

    @Parameter
    private String userName;

    @Parameter
    private String password;

    @Parameter
    private String nonProxy;

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(final Integer port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getNonProxy() {
        return nonProxy;
    }
}
