package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

public interface KarmaRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultKarmaRunner extends NodeTaskExecutor implements KarmaRunner {
    static final String TASK_NAME = "karma";
    static final String TASK_LOCATION = "/node_modules/karma/bin/karma";

    DefaultKarmaRunner(Platform platform, File nodeInstallDirectory, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, nodeInstallDirectory, workingDirectory, platform, Arrays.asList("--no-colors"));
    }
}
