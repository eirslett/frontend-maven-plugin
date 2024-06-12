package com.github.eirslett.maven.plugins.frontend.lib;

public interface CorepackRunner extends NodeTaskRunner {}

final class DefaultCorepackRunner extends NodeTaskExecutor implements CorepackRunner {
    static final String TASK_NAME = "corepack";

    public DefaultCorepackRunner(NodeExecutorConfig config) {
        super(config, TASK_NAME, config.getCorepackPath().getAbsolutePath());

        if (!config.getCorepackPath().exists()) {
            setTaskLocation(config.getCorepackPath().getAbsolutePath());
        }
    }
}
