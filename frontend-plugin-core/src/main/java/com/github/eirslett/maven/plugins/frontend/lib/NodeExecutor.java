package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;

final class NodeExecutor {
    private final ProcessExecutor executor;

    private final boolean local;
    
//    public NodeExecutor(File workingDirectory, List<String> arguments, Platform platform){
//    	this(workingDirectory, arguments, platform, false);
//    }
//
    public NodeExecutor(File workingDirectory, List<String> arguments, Platform platform, boolean local) {
    	this.local = local;
		final String node = local ? "node" : workingDirectory + Utils.normalize("/node/node");
    	this.executor = new ProcessExecutor(workingDirectory, Utils.prepend(node, arguments), platform);
    }

    public String executeAndGetResult() throws ProcessExecutionException {
        return executor.executeAndGetResult();
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        return executor.executeAndRedirectOutput(logger);
    }
}
