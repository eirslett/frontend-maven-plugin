package com.github.eirslett.maven.plugins.frontend.lib;

public interface WebpackRunner extends NodeTaskRunner {}

final class DefaultWebpackRunner extends NodeTaskExecutor implements WebpackRunner {

    private static final String TASK_LOCATION = "node_modules/webpack/bin/webpack.js";

    DefaultWebpackRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
