package com.github.eirslett.maven.plugins.frontend.lib;

import static com.github.eirslett.maven.plugins.frontend.lib.Utils.normalize;

import java.io.File;

public interface GulpRunner  extends NodeTaskRunner {}

final class DefaultGulpRunner extends NodeTaskExecutor implements GulpRunner {
    private static final String TASK_LOCATION_GULP_CLI = "node_modules/gulp-cli/bin/gulp.js";
    private static final String TASK_LOCATION_GULP = "node_modules/gulp/bin/gulp.js";

    DefaultGulpRunner(NodeExecutorConfig config) {
        super(config, getCorrectTaskLocation(config));
    }

    private static String getAbsoluteTaskLocation(NodeExecutorConfig config, String taskLocation)
    {
        String location = normalize(taskLocation);
        if (Utils.isRelative(taskLocation)) {
            File taskFile = new File(config.getWorkingDirectory(), location);
            if (!taskFile.exists()) {
                taskFile = new File(config.getInstallDirectory(), location);
            }
            location = taskFile.getAbsolutePath();
        }
        return location;
    }

    private static String getCorrectTaskLocation(NodeExecutorConfig config)
    {
        String gulpCliLocation = getAbsoluteTaskLocation(config, TASK_LOCATION_GULP_CLI);
        File gulpCliFile = new File(gulpCliLocation);
        if (gulpCliFile.exists() && gulpCliFile.canRead()) {
            return TASK_LOCATION_GULP_CLI;
        } else {
            return TASK_LOCATION_GULP;
        }
    }
}
