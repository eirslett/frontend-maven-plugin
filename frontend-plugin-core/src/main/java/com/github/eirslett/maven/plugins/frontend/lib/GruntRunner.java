package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.Arrays;

public interface GruntRunner {
    void execute(String args) throws TaskRunnerException;
}

final class DefaultGruntRunner extends NodeTaskExecutor implements GruntRunner {

    private static final String TASK_LOCATION = "node_modules/grunt-cli/bin/grunt";

    DefaultGruntRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION, Arrays.asList("--no-color"));
    }
}
