package com.github.eirslett.maven.plugins.frontend.lib;

public interface ProtractorRunner extends NodeTaskRunner {}

final class DefaultProtractorRunner extends NodeTaskExecutor implements ProtractorRunner {

    static final String TASK_LOCATION = "node_modules/protractor/bin/protractor";

    DefaultProtractorRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
