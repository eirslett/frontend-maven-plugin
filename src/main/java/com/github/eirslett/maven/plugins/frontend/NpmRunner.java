package com.github.eirslett.maven.plugins.frontend;

import org.slf4j.Logger;

import java.io.File;

public final class NpmRunner extends NodeTaskExecutor {
    static final String TASK_NAME = "npm";
    static final String TASK_LOCATION = "/node/npm/bin/npm-cli.js";

    public NpmRunner(Logger logger, Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, logger);
    }
}
