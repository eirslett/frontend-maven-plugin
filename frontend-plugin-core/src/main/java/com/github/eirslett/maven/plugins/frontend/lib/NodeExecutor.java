package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

final class NodeExecutor {
    private final ProcessExecutor executor;

    public NodeExecutor(File workingDirectory, List<String> arguments, Platform platform){
        final String node = workingDirectory + Utils.normalize("/node/node");
        this.executor = new ProcessExecutor(workingDirectory, Utils.prepend(node, arguments), platform);
    }

    public String executeAndGetResult() throws ProcessExecutionException {
        return executor.executeAndGetResult();
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }

    public NodeExecutor useEnv(Map<String, String> env) {
        this.executor.useEnv(env);
        return this;
    }
}
