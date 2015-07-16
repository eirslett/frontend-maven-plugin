package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.ArrayList;

public interface WebpackRunner {
    void execute(String args) throws TaskRunnerException;
}

final class DefaultWebpackRunner extends NodeTaskExecutor implements WebpackRunner {

    private static final String TASK_LOCATION = "node_modules/webpack/bin/webpack.js";

    DefaultWebpackRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION, new ArrayList<String>());
    }
}
