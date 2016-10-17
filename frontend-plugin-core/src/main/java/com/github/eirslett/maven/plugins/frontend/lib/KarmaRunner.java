package com.github.eirslett.maven.plugins.frontend.lib;

public interface KarmaRunner extends NodeTaskRunner {}

final class DefaultKarmaRunner extends NodeTaskExecutor implements KarmaRunner {

    static final String TASK_LOCATION = "node_modules/karma/bin/karma";

    DefaultKarmaRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
