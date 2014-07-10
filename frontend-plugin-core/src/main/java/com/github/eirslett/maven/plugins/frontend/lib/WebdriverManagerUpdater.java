package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Arrays;



public interface WebdriverManagerUpdater {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultWebdriverManagerUpdater extends NodeTaskExecutor implements WebdriverManagerUpdater {
    static final String TASK_NAME = "webdriver-update";
    static final String TASK_LOCATION = "/node_modules/protractor/bin/webdriver-manager";

    DefaultWebdriverManagerUpdater(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, Arrays.asList("update"));
    }
}
