package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultFileDownloaderTest {
    @RegisterExtension
    static WireMockExtension downloadMock = WireMockExtension.newInstance().build();
    @RegisterExtension
    static WireMockExtension secureMock = WireMockExtension.newInstance()
            .options(wireMockConfig().httpsPort(44443).keystorePath("testkeystore.jks"))
            .build();

    FileDownloader fileDownloader;
    String downloadUrl;
    String destination;

    private static MappingBuilder defaultFileDownloadStub(final MappingBuilder mappingBuilder) {
        return mappingBuilder
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("File EXE".getBytes(UTF_8)));
    }

    private static MappingBuilder defaultFileDownloadStub() {
        return defaultFileDownloadStub(get("/path/file.exe"));
    }

    private static MappingBuilder defaultFileDownloadStubWithBasicAuth(final String userName, final String password) {
        return defaultFileDownloadStub(get("/path/file.exe")
                .withBasicAuth(userName, password));
    }

    private void assertDestinationFileContent() throws IOException {
        assertThat(new String(Files.readAllBytes(Paths.get(destination)), UTF_8), equalTo("File EXE"));
    }

    private void verifyRequestedMock(WireMockExtension downloadMock) {
        downloadMock.verify(exactly(1), getRequestedFor(urlEqualTo("/path/file.exe")));
    }

    @BeforeEach
    void setUp(@TempDir final Path tempDir) {
        fileDownloader = new DefaultFileDownloader(new ProxyConfig(emptyList()));
        downloadUrl = "http://localhost:" + downloadMock.getPort() + "/path/file.exe";
        destination = tempDir.resolve("target.exe").toAbsolutePath().toString();
    }

    @Test
    void simpleFileDownload() throws DownloadException, IOException {
        // setup:
        downloadMock.stubFor(defaultFileDownloadStub());

        // when:
        fileDownloader.download(downloadUrl, destination, null, null, false);

        // then:
        assertDestinationFileContent();
        verifyRequestedMock(downloadMock);
    }

    @Test
    void secureFileDownload() throws DownloadException, IOException {
        // setup:
        secureMock.stubFor(defaultFileDownloadStub());

        // and:
        final String downloadUrl = "https://localhost:44443/path/file.exe";

        // expect: 'secure download will fail because of untrusted self-signed certificate'
        try {
            fileDownloader.download(downloadUrl, destination, null, null, false);
            fail("Error on insecure HTTPS download expected.");
        } catch (final DownloadException e) {
            assertThat(e.getMessage(), equalTo("Could not download " + downloadUrl));
        }

        // when: 'try again by trusting self-signed certificates'
        fileDownloader.download(downloadUrl, destination, null, null, true);

        // then:
        assertDestinationFileContent();
        verifyRequestedMock(secureMock);
    }

    @Test
    void fileDownloadWithBasicAuth() throws DownloadException, IOException {
        // setup:
        downloadMock.stubFor(defaultFileDownloadStubWithBasicAuth("USERNAME", "PASSWORD"));

        // when:
        fileDownloader.download(downloadUrl, destination, "USERNAME", "PASSWORD", false);

        // then:
        assertDestinationFileContent();
        verifyRequestedMock(downloadMock);
    }

    @Test
    void fileDownloadOverProxy() throws DownloadException, IOException {
        // setup:
        downloadMock.stubFor(defaultFileDownloadStub());

        // and:
        final String downloadUrl = "http://example.com/path/file.exe";

        // and:
        final ProxyConfig.Proxy proxy = new ProxyConfig.Proxy("Proxy ID", "http", "localhost", downloadMock.getPort(), null, null, "google.com");
        final FileDownloader fileDownloader = new DefaultFileDownloader(new ProxyConfig(singletonList(proxy)));

        // when:
        fileDownloader.download(downloadUrl, destination, null, null, false);

        // then:
        assertDestinationFileContent();
        verifyRequestedMock(downloadMock);
    }

    @Test
    void fileDownloadOverProxyWithAuthorization() throws DownloadException, IOException {
        // setup:
        downloadMock.stubFor(defaultFileDownloadStubWithBasicAuth("USERNAME", "PASSWORD"));

        // and:
        final String downloadUrl = "http://example.com/path/file.exe";

        // and:
        final ProxyConfig.Proxy proxy = new ProxyConfig.Proxy("Proxy ID", "http", "localhost", downloadMock.getPort(), "USERNAME", "PASSWORD", "google.com");
        final FileDownloader fileDownloader = new DefaultFileDownloader(new ProxyConfig(singletonList(proxy)));

        // when:
        fileDownloader.download(downloadUrl, destination, null, null, true);

        // then:
        assertDestinationFileContent();
        verifyRequestedMock(downloadMock);
    }

    @Test
    void downloadFromFileUri(@TempDir final Path tempDir) throws DownloadException, IOException {
        // setup:
        final Path localFileToDownload = tempDir.resolve("file.exe");
        Files.write(localFileToDownload, "File EXE".getBytes(UTF_8));
        final String downloadUrl = localFileToDownload.toUri().toString();

        // when:
        fileDownloader.download(downloadUrl, destination, null, null, false);

        // then:
        assertDestinationFileContent();
    }

    @Test
    void unavailableDownloadFails() {
        // expect:
        try {
            fileDownloader.download("http://localhost:" + downloadMock.getPort() + "/path/file.exe", destination, null, null, false);
        } catch (final DownloadException e) {
            assertThat(e.getMessage(), equalTo("Got error code 404 from the server."));
            verifyRequestedMock(downloadMock);
        }
    }
}
