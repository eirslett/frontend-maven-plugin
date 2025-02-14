package com.github.eirslett.maven.plugins.frontend.lib;

@FunctionalInterface
public interface ThrowingConsumer {
    void invoke() throws Exception;
}
