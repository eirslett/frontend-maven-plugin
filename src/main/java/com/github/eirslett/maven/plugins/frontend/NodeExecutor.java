package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.Utils.normalize;

final class NodeExecutor {
    private final ProcessExecutor executor;

    public NodeExecutor(File workingDirectory, List<String> command){
        final String node = workingDirectory + normalize("/node/node");
        this.executor = new ProcessExecutor(workingDirectory, Utils.prepend(node, command));
    }

    public String executeAndGetResult(){
        return executor.executeAndGetResult();
    }

    public int executeAndRedirectOutput(final Log logger){
        return executor.executeAndRedirectOutput(logger);
    }
}
