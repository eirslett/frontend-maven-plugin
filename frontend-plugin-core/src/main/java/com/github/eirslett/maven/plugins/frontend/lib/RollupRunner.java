package com.github.eirslett.maven.plugins.frontend.lib;

public interface RollupRunner  extends NodeTaskRunner {}

final class DefaultRollupRunner extends NodeTaskExecutor implements RollupRunner {
    private static final String TASK_LOCATION = "node_modules/rollup/bin/rollup";

    DefaultRollupRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
