package com.github.eirslett.maven.plugins.frontend.lib;

public interface RspackRunner extends NodeTaskRunner {}

final class DefaultRspackRunner extends NodeTaskExecutor implements RspackRunner {

    private static final String TASK_LOCATION = "node_modules/@rspack/cli/bin/rspack";

    DefaultRspackRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
