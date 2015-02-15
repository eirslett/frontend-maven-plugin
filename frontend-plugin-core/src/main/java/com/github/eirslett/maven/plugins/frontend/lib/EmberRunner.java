package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.ArrayList;

public interface EmberRunner {
    public void execute(String arguments) throws TaskRunnerException;
}

final class DefaultEmberRunner extends NodeTaskExecutor implements EmberRunner {
    private static final String TASK_NAME = "ember";
    private static final String TASK_LOCATION = "/node_modules/ember-cli/bin/ember";

    DefaultEmberRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, new ArrayList<String>());
    }
}
