package com.github.eirslett.maven.plugins.frontend.lib;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class DefaultNpmRunnerTest {

    private final String id = "id";
    private final String protocol = "http";
    private final String host = "localhost";
    private final int port = 8888;
    private final String username = "someusername";
    private final String password = "somepassword";
    private final String nonProxyHosts = "www.google.ca|www.google.com|*.google.de";
    private final String[] expectedNonProxyHosts = new String[] {"www.google.ca","www.google.com",".google.de"};
    private final String registryUrl = "www.npm.org";
    private final String expectedUrl = "http://someusername:somepassword@localhost:8888";

    @Test
    public void buildArguments_basicTest() {
        List<String> strings = runBuildArguments(nonProxyHosts, registryUrl);

        assertThat(strings, CoreMatchers.hasItem("--proxy=" + expectedUrl));
        assertThat(strings, CoreMatchers.hasItem("--https-proxy=" + expectedUrl));
        for (String expectedNonProxyHost: expectedNonProxyHosts) {
            assertThat(strings, CoreMatchers.hasItem("--noproxy=" + expectedNonProxyHost));
        }
        assertThat(strings, CoreMatchers.hasItem("--registry=" + registryUrl));
        assertEquals(6, strings.size());
    }


    @Test
    public void buildArguments_emptyRegistryUrl() {
        List<String> strings = runBuildArguments(nonProxyHosts, "");

        assertThat(strings, CoreMatchers.hasItem("--proxy=" + expectedUrl));
        assertThat(strings, CoreMatchers.hasItem("--https-proxy=" + expectedUrl));
        for (String expectedNonProxyHost: expectedNonProxyHosts) {
            assertThat(strings, CoreMatchers.hasItem("--noproxy=" + expectedNonProxyHost));
        }
        assertEquals(5, strings.size());
    }

    @Test
    public void buildArguments_nullRegistryUrl() {
        List<String> strings = runBuildArguments(nonProxyHosts, null);

        assertThat(strings, CoreMatchers.hasItem("--proxy=" + expectedUrl));
        assertThat(strings, CoreMatchers.hasItem("--https-proxy=" + expectedUrl));
        for (String expectedNonProxyHost: expectedNonProxyHosts) {
            assertThat(strings, CoreMatchers.hasItem("--noproxy=" + expectedNonProxyHost));
        }
        assertEquals(5, strings.size());
    }

    @Test
    public void buildArguments_emptyNoProxy() {
        List<String> strings = runBuildArguments("", "");

        assertThat(strings, CoreMatchers.hasItem("--proxy=" + expectedUrl));
        assertThat(strings, CoreMatchers.hasItem("--https-proxy=" + expectedUrl));
        assertEquals(2, strings.size());
    }

    @Test
    public void buildArguments_nullNoProxy() {
        List<String> strings = runBuildArguments(null, "");

        assertThat(strings, CoreMatchers.hasItem("--proxy=" + expectedUrl));
        assertThat(strings, CoreMatchers.hasItem("--https-proxy=" + expectedUrl));
        assertEquals(2, strings.size());
    }

    private List<String> runBuildArguments(String nonProxyHost, String registryUrl) {
        List<ProxyConfig.Proxy> proxyList = Stream.of(
                new ProxyConfig.Proxy(id, protocol, host, port, username, password, nonProxyHost)
        ).collect(Collectors.toList());

        return DefaultNpmRunner.buildArguments(new ProxyConfig(proxyList), registryUrl);
    }
}
