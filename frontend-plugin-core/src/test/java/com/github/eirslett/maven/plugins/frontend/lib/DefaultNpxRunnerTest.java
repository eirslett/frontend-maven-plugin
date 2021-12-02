package com.github.eirslett.maven.plugins.frontend.lib;

import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class DefaultNpxRunnerTest {

    private final String registryUrl = "www.npm.org";

    @Test
    public void buildArgument_basicTest() {
        List<String> arguments = DefaultNpxRunner.buildNpmArguments(new ProxyConfig(Collections.emptyList()), null);
        Assert.assertEquals(0, arguments.size());
    }

    @Test
    public void buildArgument_withRegistryUrl() {
        List<String> arguments = DefaultNpxRunner.buildNpmArguments(new ProxyConfig(Collections.emptyList()), registryUrl);
        Assert.assertEquals(2, arguments.size());
        assertThat(arguments, CoreMatchers.hasItems("--", "--registry=" + registryUrl));
    }
}
