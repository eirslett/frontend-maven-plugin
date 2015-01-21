package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

public interface GruntRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultGruntRunner extends NodeTaskExecutor implements GruntRunner {
    private static final String TASK_NAME = "grunt";
    private static final String TASK_LOCATION = "/node_modules/grunt-cli/bin/grunt";

    DefaultGruntRunner(Platform platform, File nodeInstallDirectory, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, nodeInstallDirectory, workingDirectory, platform, Arrays.asList("--no-color"));
    }
}
