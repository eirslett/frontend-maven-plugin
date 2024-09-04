package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

final class DownloadException extends Exception {
    public DownloadException(String message){
        super(message);
    }
    DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}

interface FileDownloader {
    void download(String downloadUrl, String destination, String userName, String password, Map<String, String> header) throws DownloadException;
}

final class DefaultFileDownloader implements FileDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloader.class);

    private final ProxyConfig proxyConfig;

    public DefaultFileDownloader(ProxyConfig proxyConfig){
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void download(String downloadUrl, String destination, String userName, String password, Map<String, String> httpHeaders) throws DownloadException {
        // force tls to 1.2 since github removed weak cryptographic standards
        // https://blog.github.com/2018-02-02-weak-cryptographic-standards-removal-notice/
        System.setProperty("https.protocols", "TLSv1.2");
        String fixedDownloadUrl = downloadUrl;
        try {
            fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
            URI downloadURI = new URI(fixedDownloadUrl);
            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                FileUtils.copyFile(new File(downloadURI), new File(destination));
            }
            else {
                CloseableHttpResponse response = execute(fixedDownloadUrl, userName, password, httpHeaders);
                int statusCode = response.getStatusLine().getStatusCode();
                if(statusCode != 200){
                    throw new DownloadException("Got error code "+ statusCode +" from the server.");
                }
                
                byte[] data = IOUtils.toByteArray(response.getEntity().getContent());
                new File(FilenameUtils.getFullPathNoEndSeparator(destination)).mkdirs();
                FileUtils.writeByteArrayToFile(new File(destination), data);
            }
        } catch (IOException | URISyntaxException e) {
            throw new DownloadException("Could not download " + fixedDownloadUrl, e);
        }
    }

    private CloseableHttpResponse execute(String requestUrl, String userName, String password, Map<String, String> httpHeaders) throws IOException {
        final HttpGet request = new HttpGet(requestUrl);
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        final Proxy proxy = proxyConfig.getProxyForUrl(requestUrl);
        if (proxy != null) {
            LOGGER.info("Downloading via proxy " + proxy.toString());

            final RequestConfig requestConfig = RequestConfig.custom()
                .setProxy(new HttpHost(proxy.host, proxy.port))
                .build();
            request.setConfig(requestConfig);

            if (proxy.useAuthentication()) {
                credentialsProvider.setCredentials(
                    new AuthScope(proxy.host, proxy.port),
                    new UsernamePasswordCredentials(proxy.username, proxy.password));
            }
        } else {
            LOGGER.info("No proxy was configured, downloading directly");
        }
        
        if (httpHeaders != null) {
            for (Map.Entry<String, String> header : httpHeaders.entrySet()) {
                LOGGER.info("Using HTTP-Header (" + header.getKey() + ") from settings.xml");
                request.addHeader(header.getKey(), header.getValue());
            }
        }

        if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
            LOGGER.info("Using credentials (" + userName + ") from settings.xml");
            // Auth target host
            URL targetUrl = new URL(requestUrl);
            credentialsProvider.setCredentials(
                new AuthScope(targetUrl.getHost(), targetUrl.getPort()),
                new UsernamePasswordCredentials(userName, password));
            final HttpClientContext localContext = makeLocalContext(targetUrl);

            return buildHttpClient(credentialsProvider)
                .execute(request, localContext);
        }

        return buildHttpClient(credentialsProvider).execute(request);
    }

    private HttpClientContext makeLocalContext(URL requestUrl) {
        // Auth target host
        HttpHost target = new HttpHost (requestUrl.getHost(), requestUrl.getPort(), requestUrl.getProtocol());
        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);
        // Add AuthCache to the execution context
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
        return localContext;
    }

    private CloseableHttpClient buildHttpClient(CredentialsProvider credentialsProvider) {
        return HttpClients.custom()
                .disableContentCompression()
                .useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }
}
