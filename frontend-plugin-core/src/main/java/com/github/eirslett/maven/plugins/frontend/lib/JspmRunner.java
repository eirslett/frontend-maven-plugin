package com.github.eirslett.maven.plugins.frontend.lib;

public interface JspmRunner extends NodeTaskRunner {}

final class DefaultJspmRunner extends NodeTaskExecutor implements JspmRunner {

    static final String TASK_LOCATION = "node_modules/.bin/jspm";

    DefaultJspmRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }

}
