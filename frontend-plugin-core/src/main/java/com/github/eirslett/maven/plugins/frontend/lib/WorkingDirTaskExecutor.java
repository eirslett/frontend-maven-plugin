package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.List;

public class WorkingDirTaskExecutor extends NodeTaskExecutor {

    private final File workingDirectory;
    public WorkingDirTaskExecutor(String taskName, String taskLocation, File nodeInstallDirectory,
            File workingDirectory, Platform platform, List<String> additionalArguments) {
        super(taskName, taskLocation, nodeInstallDirectory, workingDirectory, platform, additionalArguments);
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    protected File getTaskInstallDirectory() {
        return workingDirectory;
    }

}
