package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public String executeAndGetResult() throws ProcessExecutionException {
        return executor.executeAndGetResult();
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }
}
