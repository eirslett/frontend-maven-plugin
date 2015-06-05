package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.ArrayList;

public interface JspmRunner {

    public void execute(String args) throws TaskRunnerException;
}


final class DefaultJspmRunner extends NodeTaskExecutor implements JspmRunner {

    static final String TASK_NAME = "jspm";
    static final String TASK_LOCATION = "/node_modules/jspm/jspm.js";

    public DefaultJspmRunner(Platform platform, File workingDirectory) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, new ArrayList<String>());
    }

}
