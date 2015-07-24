package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

final class NodeExecutor {
    private final ProcessExecutor executor;

    public NodeExecutor(NodeExecutorConfig config, List<String> arguments){
        final String node = config.getNodePath().getAbsolutePath();
        List<String> localPaths = new ArrayList<String>();
        localPaths.add(config.getNodePath().getParent());
        localPaths.add(config.getNpmPath().getParent());
        this.executor = new ProcessExecutor(
            config.getWorkingDirectory(),
            localPaths,
            Utils.prepend(node, arguments),
            config.getPlatform());
    }

    public String executeAndGetResult() throws ProcessExecutionException {
        return executor.executeAndGetResult();
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }
}
