package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;

/**
 * Created by Malone on 7/8/14.
 */
public interface WebdriverManagerRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultWebdriverManagerRunner extends NodeTaskExecutor implements WebdriverManagerRunner {
    static final String TASK_NAME = "webdriver-start";
    static final String TASK_LOCATION = "/node_modules/protractor/bin/webdriver-manager";

    DefaultWebdriverManagerRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, Arrays.asList("start"));
    }
}
