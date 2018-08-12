package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface ExecutorConfig {
    File getWorkingDirectory();
    Platform getPlatform();
}
