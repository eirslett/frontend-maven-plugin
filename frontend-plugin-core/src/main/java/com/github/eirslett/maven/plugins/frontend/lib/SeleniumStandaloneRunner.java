package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

/**
 * Created by Malone on 6/25/14.
 */
public interface SeleniumStandaloneRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultSeleniumStandaloneRunner extends NodeTaskExecutor implements SeleniumStandaloneRunner {
    private static final String TASK_NAME = "selenium-standalone";
    private static final String TASK_LOCATION = "/node_modules/selenium-standalone/bin/start-selenium";

    DefaultSeleniumStandaloneRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, Arrays.asList("--no-color"));
    }
}
