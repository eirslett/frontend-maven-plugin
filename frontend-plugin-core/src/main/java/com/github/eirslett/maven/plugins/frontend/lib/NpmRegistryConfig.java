package com.github.eirslett.maven.plugins.frontend.lib;

public class NpmRegistryConfig {
    private final String url;
    private final String username;
    private final String password;

    public NpmRegistryConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
