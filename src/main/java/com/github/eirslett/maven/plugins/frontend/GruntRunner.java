package com.github.eirslett.maven.plugins.frontend;

import org.slf4j.Logger;

import java.io.File;

public final class GruntRunner extends NodeTaskExecutor {
    static final String TASK_NAME = "grunt";
    static final String TASK_LOCATION = "/node_modules/grunt-cli/bin/grunt";

    public GruntRunner(Logger logger, Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, logger);
    }
}
