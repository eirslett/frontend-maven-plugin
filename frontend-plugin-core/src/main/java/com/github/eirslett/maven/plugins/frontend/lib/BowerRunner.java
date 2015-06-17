package com.github.eirslett.maven.plugins.frontend.lib;

public interface BowerRunner {
    void execute(String args) throws TaskRunnerException;
}

final class DefaultBowerRunner extends NodeTaskExecutor implements BowerRunner {

    private static final String TASK_LOCATION = "node_modules/bower/bin/bower";

    DefaultBowerRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
