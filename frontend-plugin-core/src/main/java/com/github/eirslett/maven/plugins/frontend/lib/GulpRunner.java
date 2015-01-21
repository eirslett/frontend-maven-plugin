package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

public interface GulpRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultGulpRunner extends NodeTaskExecutor implements GulpRunner {
    private static final String TASK_NAME = "gulp";
    private static final String TASK_LOCATION = "/node_modules/gulp/bin/gulp.js";

    DefaultGulpRunner(Platform platform, File nodeInstallDirectory, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, nodeInstallDirectory, workingDirectory, platform, Arrays.asList("--no-color"));
    }
}
