package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface VitestRunner extends NodeTaskRunner {}

final class DefaultVitestRunner extends NodeTaskExecutor implements VitestRunner {

    static final String TASK_LOCATION = "node_modules/vitest/vitest.mjs";
    static final String LEGACY_TASK_LOCATION = "node_modules/vitest/dist/cli.js";

    DefaultVitestRunner(NodeExecutorConfig config) {
        super(config, "vitest", TASK_LOCATION);
        if (!taskExists(config, TASK_LOCATION) && taskExists(config, LEGACY_TASK_LOCATION)) {
            setTaskLocation(LEGACY_TASK_LOCATION);
        }
    }

    private static boolean taskExists(NodeExecutorConfig config, String location) {
        File inWorkingDirectory = new File(config.getWorkingDirectory(), location);
        if (inWorkingDirectory.exists()) {
            return true;
        }
        return new File(config.getInstallDirectory(), location).exists();
    }
}
