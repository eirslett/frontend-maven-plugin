package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.io.File;

public interface KarmaRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultKarmaRunner extends NodeTaskExecutor implements KarmaRunner {
    static final String TASK_NAME = "karma";
    static final String TASK_LOCATION = "/node_modules/karma/bin/karma";

    DefaultKarmaRunner(Logger logger, Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, logger);
    }
}
