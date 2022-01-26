package com.github.eirslett.maven.plugins.frontend.mojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Mojo(name = "npm-login", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class NpmLoginMojo extends AbstractMojo {

    @Parameter(property = "frontend.npm-login.npmInheritsProxyConfigFromMaven", defaultValue = "true")
    private boolean npmInheritsProxyConfigFromMaven;

    /**
     * Registry override, passed as the registry option during npm install if set.
     */
    @Parameter(property = "npmRegistryURL", required = true)
    private String npmRegistryURL;

    /**
     * Server Id for access to npm registry
     */
    @Parameter(property = "npmRegistryServerId", required = true)
    private String npmRegistryServerId;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.npm-login", defaultValue = "${skip.npm-login}")
    private boolean skip;

    @Parameter(property = "userHome", defaultValue = "${user.home}")
    private String userHome;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }

        String npmRegistryAuth = "//" + npmRegistryURL
                .replaceAll("http.?://", "")
                .replaceFirst("/$", "") + "/";
        try {
            Path npmrcPath = Paths.get(userHome, ".npmrc");
            int position = -1;
            List<String> lines;
            if (Files.exists(npmrcPath)) {
                lines = Files.readAllLines(npmrcPath);
                int i = -1;
                for (String line : lines) {
                    if (line.startsWith(npmRegistryAuth)) {
                        getLog().info("Token exists. Skipping execution.");
                        return;
                    }

                    i++;
                    if (position == -1 && line.startsWith("//") && line.contains(":_authToken=")) {
                        position = i;
                    }
                }
            } else {
                lines = new ArrayList<>();
                position = 0;
                Files.createFile(npmrcPath);
            }
            lines.add(Math.max(position, 0), npmRegistryAuth + ":_authToken=\"" + getToken() + "\"");
            Files.write(npmrcPath, lines);
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw MojoUtils.toMojoFailureException(e);
        }
    }


    private String getToken() throws Exception {
        Server server = MojoUtils.decryptServer(npmRegistryServerId, session, decrypter);
        if (server == null) {
            throw new IllegalStateException("Unknown serverId: " + npmRegistryServerId);
        }
        String npmRegistryUsername = server.getUsername();
        String npmRegistryPassword = server.getPassword();

        ProxyConfig proxyConfig;
        if (npmInheritsProxyConfigFromMaven) {
            proxyConfig = MojoUtils.getProxyConfig(session, decrypter);
        } else {
            getLog().info("npm-login not inheriting proxy config from Maven");
            proxyConfig = new ProxyConfig(Collections.emptyList());
        }

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .disableContentCompression()
                .useSystemProperties();
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        Proxy proxy = proxyConfig.getProxyForUrl(npmRegistryURL);

        if (proxy != null) {
            if (proxy.useAuthentication()) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(proxy.host, proxy.port),
                        new UsernamePasswordCredentials(proxy.username, proxy.password)
                );

                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            HttpHost proxyHttpHost = new HttpHost(proxy.host, proxy.port);
            requestConfigBuilder.setProxy(proxyHttpHost);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        HttpPut httpPut = new HttpPut(npmRegistryURL.replaceFirst("/$", "") + "/-/user/org.couchdb.user:" + npmRegistryUsername);
        httpPut.setConfig(requestConfigBuilder.build());
        TokenRequest tokenRequest = new TokenRequest(npmRegistryUsername, npmRegistryPassword);
        httpPut.setEntity(new StringEntity(objectMapper.writeValueAsString(tokenRequest), APPLICATION_JSON));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(httpPut);
            TokenResponse tokenResponse = objectMapper.readValue(response.getEntity().getContent(), TokenResponse.class);
            if (tokenResponse.getError() != null) {
                throw new MojoFailureException("Error get token: " + tokenResponse.getError());
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
