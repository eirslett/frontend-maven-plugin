package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

final class NodeExecutor {
    private final ProcessExecutor executor;

    public NodeExecutor(NodeExecutorConfig config, List<String> arguments, Map<String, String> additionalEnvironment){
        final String node = config.getNodePath().getAbsolutePath();
        List<String> localPaths = new ArrayList<String>();
        localPaths.add(config.getNodePath().getParent());
        this.executor = new ProcessExecutor(
            config.getWorkingDirectory(),
            localPaths,
            Utils.prepend(node, arguments),
            config.getPlatform(),
            additionalEnvironment);
    }

    public String executeAndGetResult(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndGetResult(logger);
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }
}
