package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

public interface WebpackRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultWebpackRunner extends NodeTaskExecutor implements WebpackRunner {
    private static final String TASK_NAME = "webpack";
    private static final String TASK_LOCATION = "/node_modules/webpack/bin/webpack.js";

    DefaultWebpackRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, Arrays.asList("--no-color"));
    }
}
