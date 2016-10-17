package com.github.eirslett.maven.plugins.frontend.lib;

public interface GulpRunner  extends NodeTaskRunner {}

final class DefaultGulpRunner extends NodeTaskExecutor implements GulpRunner {
    private static final String TASK_LOCATION = "node_modules/gulp/bin/gulp.js";

    DefaultGulpRunner(NodeExecutorConfig config) {
        super(config, TASK_LOCATION);
    }
}
