package com.github.eirslett.maven.plugins.frontend.lib;


import java.util.Collections;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultNpxRunnerTest {

    private final String registryUrl = "www.npm.org";

    @Test
    public void buildArgument_basicTest() {
        List<String> arguments = DefaultNpxRunner.buildNpmArguments(new ProxyConfig(Collections.emptyList()), null);
        Assertions.assertEquals(0, arguments.size());
    }

    @Test
    public void buildArgument_withRegistryUrl() {
        List<String> arguments = DefaultNpxRunner.buildNpmArguments(new ProxyConfig(Collections.emptyList()), registryUrl);
        Assertions.assertEquals(2, arguments.size());
        assertThat(arguments, CoreMatchers.hasItems("--", "--registry=" + registryUrl));
    }
}
