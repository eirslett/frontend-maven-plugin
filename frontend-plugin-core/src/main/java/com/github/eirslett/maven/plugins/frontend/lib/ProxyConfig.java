package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;

public class ProxyConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfig.class);

    private final List<Proxy> proxies;

    public ProxyConfig(List<Proxy> proxies) {
        this.proxies = proxies;
    }

    public boolean isEmpty() {
        return proxies.isEmpty();
    }

    public Proxy getProxyForUrl(String requestUrl) {
        if (proxies.isEmpty()) {
            LOGGER.info("No proxies configured");
            return null;
        }
        final URI uri = URI.create(requestUrl);
        for (Proxy proxy : proxies) {
            if (!proxy.isNonProxyHost(uri.getHost())) {
                return proxy;
            }
        }
        LOGGER.info("Could not find matching proxy for host: {}", uri.getHost());
        return null;
    }

    public Proxy getSecureProxy() {
        for (Proxy proxy : proxies) {
            if (proxy.isSecure()) {
                return proxy;
            }
        }
        return null;
    }

    public Proxy getInsecureProxy() {
        for (Proxy proxy : proxies) {
            if (!proxy.isSecure()) {
                return proxy;
            }
        }
        return null;
    }

    public static class Proxy {
        public final String id;
        public final String protocol;
        public final String host;
        public final int port;
        public final String username;
        public final String password;

        public final String nonProxyHosts;

        public Proxy(String id, String protocol, String host, int port, String username, String password, String nonProxyHosts) {
            this.host = host;
            this.id = id;
            this.protocol = protocol;
            this.port = port;
            this.username = username;
            this.password = password;
            this.nonProxyHosts = nonProxyHosts;
        }

        public boolean useAuthentication(){
            return username != null && !username.isEmpty();
        }

        public URI getUri() {
            String authentication = useAuthentication() ? username + ":" + password : null;
            try {
                // Proxies should be schemed with http, even if the protocol is https
                return new URI("http", authentication, host, port, null, null, null);
            } catch (URISyntaxException e) {
                throw new ProxyConfigException("Invalid proxy settings", e);
            }
        }

        public boolean isSecure(){
            return "https".equals(protocol);
        }

        public boolean isNonProxyHost(String host) {
            if (host != null && nonProxyHosts != null && nonProxyHosts.length() > 0) {
                for (StringTokenizer tokenizer = new StringTokenizer(nonProxyHosts, "|"); tokenizer.hasMoreTokens(); ) {
                    String pattern = tokenizer.nextToken();
                    pattern = pattern.replace(".", "\\.").replace("*", ".*");
                    if (host.matches(pattern)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * As per https://docs.npmjs.com/misc/config#noproxy , npm expects a comma (`,`) separated list but
         * maven settings.xml usually specifies the no proxy hosts as a bar (`|`) separated list (see
         * http://maven.apache.org/guides/mini/guide-proxies.html) .
         *
         * We could do the conversion here but npm seems to accept the bar separated list regardless
         * of what the documentation says so we do no conversion for now.
         * @return
         */
        public String getNonProxyHosts() {
            return nonProxyHosts;
        }

        @Override
        public String toString() {
            return id + "{" +
                    "protocol='" + protocol + '\'' +
                    ", host='" + host + '\'' +
                    ", port=" + port +
                    ", nonProxyHosts='" + nonProxyHosts + '\'' +
                    (useAuthentication()? ", with username/passport authentication" : "") +
                    '}';
        }
    }

    static class ProxyConfigException extends RuntimeException {

        private ProxyConfigException(String message, Exception cause) {
            super(message, cause);
        }

    }
}
