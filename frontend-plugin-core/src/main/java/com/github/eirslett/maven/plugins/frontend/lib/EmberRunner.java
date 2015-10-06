package com.github.eirslett.maven.plugins.frontend.lib;

public interface EmberRunner extends NodeTaskRunner {}

final class DefaultEmberRunner extends NodeTaskExecutor implements EmberRunner {

    private static final String TASK_LOCATION = "node_modules/ember-cli/bin/ember";

    DefaultEmberRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
