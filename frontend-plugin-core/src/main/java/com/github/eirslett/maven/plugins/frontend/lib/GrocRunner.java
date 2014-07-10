package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Malone on 7/7/14.
 */

public interface GrocRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultGrocRunner extends NodeTaskExecutor implements GrocRunner {
    static final String TASK_NAME = "groc";
    static final String TASK_LOCATION = "/node_modules/groc/bin/groc";

    DefaultGrocRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, Arrays.asList(""));
    }

}
