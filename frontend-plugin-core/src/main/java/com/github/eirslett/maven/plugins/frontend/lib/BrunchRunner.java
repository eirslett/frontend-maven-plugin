package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.Collections;

public interface BrunchRunner {
    void execute(String args) throws TaskRunnerException;
}

final class DefaultBrunchRunner extends NodeTaskExecutor implements BrunchRunner {

    private static final String TASK_LOCATION = "node_modules/brunch/bin/brunch";

    DefaultBrunchRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION, Collections.<String>emptyList());
    }
}

