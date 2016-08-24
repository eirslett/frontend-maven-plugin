package com.github.eirslett.maven.plugins.frontend.lib;

public interface GruntRunner extends NodeTaskRunner {}

final class DefaultGruntRunner extends NodeTaskExecutor implements GruntRunner {

    private static final String TASK_LOCATION = "node_modules/.bin/grunt";

    DefaultGruntRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
