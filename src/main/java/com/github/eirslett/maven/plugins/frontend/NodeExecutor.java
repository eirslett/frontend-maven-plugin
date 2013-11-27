package com.github.eirslett.maven.plugins.frontend;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.Utils.normalize;

final class NodeExecutor {
    private final ProcessExecutor executor;

    public NodeExecutor(File workingDirectory, List<String> arguments, Platform platform){
        final String node = workingDirectory + normalize("/node/node");
        this.executor = new ProcessExecutor(workingDirectory, Utils.prepend(node, arguments), platform);
    }

    public String executeAndGetResult() throws ProcessExecutionException {
        return executor.executeAndGetResult();
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }
}
