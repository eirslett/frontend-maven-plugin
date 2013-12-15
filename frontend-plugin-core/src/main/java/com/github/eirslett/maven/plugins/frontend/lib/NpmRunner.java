package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

public interface NpmRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";
    static final String TASK_LOCATION = "/node/npm/bin/npm-cli.js";

    public DefaultNpmRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, Arrays.asList("--color=false"));
    }
}
