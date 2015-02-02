package com.github.eirslett.maven.plugins.frontend.lib;

import java.net.URI;
import java.net.URISyntaxException;

public class ProxyConfig {
    public final String id;
    public final String protocol;
    public final String host;
    public final int port;
    public final String username;
    public final String password;

    public ProxyConfig(String id, String protocol, String host, int port, String username, String password) {
        this.id = id;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public boolean useAuthentication(){
        return username != null && !username.isEmpty();
    }

    public URI getUri() {
        String authentication = useAuthentication() ? username + ":" + password : null;
        try {
            return new URI(protocol, authentication, host, port, null, null, null);
        } catch (URISyntaxException e) {
            throw new ProxyConfigException("Invalid proxy settings", e);
        }
    }

    public boolean isSecure(){
        return "https".equals(protocol);
    }

    @Override
    public String toString() {
        return id + "{" +
                "protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                (useAuthentication()? ", with username/passport authentication" : "") +
                '}';
    }

    class ProxyConfigException extends RuntimeException {

        private ProxyConfigException(String message, Exception cause) {
            super(message, cause);
        }

    }
}
