package com.github.eirslett.maven.plugins.frontend.lib;

public class ProxyConfig {
    public final String protocol;
    public final String host;
    public final int port;
    public final String username;
    public final String password;

    public ProxyConfig(String protocol, String host, int port, String username, String password) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public boolean useAuthentication(){
        return username != null && !username.isEmpty();
    }

    @Override
    public String toString() {
        return "ProxyConfig{" +
                "protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password=**********" +
                '}';
    }
}
