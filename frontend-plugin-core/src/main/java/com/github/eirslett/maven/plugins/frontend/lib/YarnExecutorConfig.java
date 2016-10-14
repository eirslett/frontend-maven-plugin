package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface YarnExecutorConfig {
    File getYarnPath();

    File getWorkingDirectory();

    Platform getPlatform();
}

final class InstallYarnExecutorConfig implements YarnExecutorConfig {

    private static final String YARN_WINDOWS =
        YarnInstaller.INSTALL_PATH.replaceAll("/", "\\\\") + "\\Yarn\\bin\\yarn.cmd";

    private static final String YARN_DEFAULT = YarnInstaller.INSTALL_PATH + "/Yarn/bin/yarn";

    private final InstallConfig installConfig;

    public InstallYarnExecutorConfig(InstallConfig installConfig) {
        this.installConfig = installConfig;
    }

    @Override
    public File getYarnPath() {
        String yarnExecutable = getPlatform().isWindows() ? YARN_WINDOWS : YARN_DEFAULT;
        return new File(this.installConfig.getInstallDirectory() + yarnExecutable);
    }

    @Override
    public File getWorkingDirectory() {
        return this.installConfig.getWorkingDirectory();
    }

    @Override
    public Platform getPlatform() {
        return this.installConfig.getPlatform();
    }
}