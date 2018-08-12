package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.isRelative;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public interface NpmRegistryAuthHandler {
    void handle(List<String> arguments, Map<String, String> environment) throws Exception;
}

class DefaultNpmRegistryAuthHandler implements NpmRegistryAuthHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpmRegistryAuthHandler.class);
    private static final String NPMRC_DEFAULT_FILE = ".npmrc";
    private static final String NPMRC_TOKEN_VARIABLE = "NPM_TOKEN";
    private static final Pattern NPMRC_PATH_PATTERN = Pattern.compile("--userconfig\\s*=?\\s*(?<path>(?:\".*?\")|(?:\\S*[^\\s]))");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorConfig executorConfig;
    private final ProxyConfig proxyConfig;
    private final NpmRegistryConfig registryConfig;

    public DefaultNpmRegistryAuthHandler(ExecutorConfig executorConfig, ProxyConfig proxyConfig, NpmRegistryConfig registryConfig) {
        this.executorConfig = executorConfig;
        this.proxyConfig = proxyConfig;
        this.registryConfig = registryConfig;
    }

    @Override
    public void handle(List<String> arguments, Map<String, String> environment) throws Exception {
        if (registryConfig == null || registryConfig.getUsername() == null) {
            LOGGER.info("NPM Registry does not require authentication");
            return;
        }

        int index = -1;
        Matcher matcher = null;
        for (int i = 0; i < arguments.size(); i++) {
            matcher = NPMRC_PATH_PATTERN.matcher(arguments.get(i));
            if (matcher.matches()) {
                index = i;
                break;
            }
        }

        final File npmrcFile;
        if (index == -1) {
            npmrcFile = new File(executorConfig.getWorkingDirectory(), NPMRC_DEFAULT_FILE);
        } else {
            final String userconfig = matcher.replaceAll("${path}").replace("\"", "");
            npmrcFile = isRelative(userconfig) ? new File(executorConfig.getWorkingDirectory(), userconfig) :
                    new File(userconfig);
        }

        final String token = getToken();

        final List<String> list;
        if (npmrcFile.exists()) {
            list = IOUtils.readLines(new FileInputStream(npmrcFile));
        } else {
            list = new ArrayList<>();
        }
        final File dstNpmrcFile = File.createTempFile(".npmrc", "");
        dstNpmrcFile.deleteOnExit();
        final String line = "//" + registryConfig.getUrl()
                .replaceAll("http.?://", "")
                .replaceFirst("/$", "") +
                "/:_authToken=${" + NPMRC_TOKEN_VARIABLE + "}";
        list.add(line);
        try (FileOutputStream ous = new FileOutputStream(dstNpmrcFile)) {
            IOUtils.writeLines(list, System.lineSeparator(), ous);
        }
        final String argument = "--userconfig=" + dstNpmrcFile.getAbsolutePath();
        if (index != -1) {
            arguments.remove(index);
            arguments.add(index, argument);
        } else {
            arguments.add(argument);
        }
        environment.put(NPMRC_TOKEN_VARIABLE, token);
    }

    private String getToken() throws Exception {
        if (registryConfig == null) {
            return null;
        }
        final HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .disableContentCompression()
                .useSystemProperties();

        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

        if (proxyConfig != null) {
            Proxy proxy = proxyConfig.getProxyForUrl(registryConfig.getUrl());
            if (proxy != null) {
                if (proxy.useAuthentication()) {
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            new AuthScope(proxy.host, proxy.port),
                            new UsernamePasswordCredentials(proxy.username, proxy.password)
                    );

                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }
                final HttpHost proxyHttpHost = new HttpHost(proxy.host, proxy.port);
                requestConfigBuilder.setProxy(proxyHttpHost);
            }
        }

        final HttpPut httpPut = new HttpPut(registryConfig.getUrl().replaceFirst("/$", "") + "/-/user/org.couchdb.user:" + registryConfig.getUsername());
        httpPut.setConfig(requestConfigBuilder.build());
        TokenRequest tokenRequest = new TokenRequest(registryConfig.getUsername(), registryConfig.getPassword());
        httpPut.setEntity(new StringEntity(objectMapper.writeValueAsString(tokenRequest), APPLICATION_JSON));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(httpPut);
            TokenResponse tokenResponse = objectMapper.readValue(response.getEntity().getContent(), TokenResponse.class);
            if (tokenResponse.getError() != null) {
                throw new IllegalStateException("Error get token: " + tokenResponse.getError());
            }
            return tokenResponse.getToken();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenRequest {
        private String name;
        private String password;

        public TokenRequest() {
        }

        public TokenRequest(String name, String password) {
            this.name = name;
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenResponse {
        private String token;
        private String error;

        public TokenResponse() {
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
