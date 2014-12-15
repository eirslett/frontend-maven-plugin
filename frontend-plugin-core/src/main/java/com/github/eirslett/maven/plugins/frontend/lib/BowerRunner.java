package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.ArrayList;

public interface BowerRunner {

    public void execute(String args) throws TaskRunnerException;
}

final class DefaultBowerRunner extends NodeTaskExecutor implements BowerRunner {

    private static final String TASK_NAME = "bower";
    private static final String TASK_LOCATION = "/node_modules/bower/bin/bower";

    DefaultBowerRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, new ArrayList<String>());
    }
}
