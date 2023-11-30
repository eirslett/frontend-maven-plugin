package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BunExecutor {
    private final ProcessExecutor executor;

    public BunExecutor(BunExecutorConfig config, List<String> arguments, Map<String, String> additionalEnvironment) {
        final String bun = config.getBunPath().getAbsolutePath();
        List<String> localPaths = new ArrayList<String>();
        localPaths.add(config.getBunPath().getParent());
        this.executor = new ProcessExecutor(
                config.getWorkingDirectory(),
                localPaths,
                Utils.prepend(bun, arguments),
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
