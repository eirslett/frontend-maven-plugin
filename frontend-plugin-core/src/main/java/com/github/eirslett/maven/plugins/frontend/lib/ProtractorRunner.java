package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

/**
 * Created by Malone on 6/25/14.
 */
public interface ProtractorRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultProtractorRunner extends NodeTaskExecutor implements ProtractorRunner {
    private static final String TASK_NAME = "protractor";
    private static final String TASK_LOCATION = "/node_modules/protractor/bin/protractor";

    DefaultProtractorRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, Arrays.asList("--no-color"));
    }
}
