package com.github.eirslett.maven.plugins.frontend;

import org.slf4j.Logger;

import java.io.File;

public class KarmaRunner extends NodeTaskExecutor {
    static final String TASK_NAME = "karma";
    static final String TASK_LOCATION = "/node_modules/karma/bin/karma";

    public KarmaRunner(Logger logger, Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, logger);
    }
}
