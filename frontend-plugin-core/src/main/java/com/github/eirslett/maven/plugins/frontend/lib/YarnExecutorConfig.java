package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface YarnExecutorConfig {

    File getNodePath();

    File getYarnPath();

    File getWorkingDirectory();

    Platform getPlatform();
}

final class InstallYarnExecutorConfig implements YarnExecutorConfig {

    private static final String YARN_WINDOWS =
        YarnInstaller.INSTALL_PATH.replaceAll("/", "\\\\") + "\\Yarn\\bin\\yarn.cmd";

    private static final String YARN_DEFAULT = YarnInstaller.INSTALL_PATH + "/dist/bin/yarn";

    private File nodePath;

    private final File yarnExecutablePath;

    private final InstallConfig installConfig;

    public InstallYarnExecutorConfig(InstallConfig installConfig) {
        this(installConfig, null);
    }

    public InstallYarnExecutorConfig(InstallConfig installConfig, File yarnExecutablePath) {
        this.installConfig = installConfig;
        this.yarnExecutablePath = yarnExecutablePath;
        nodePath = new InstallNodeExecutorConfig(installConfig).getNodePath();
    }

    @Override
    public File getNodePath() {
        return nodePath;
    }

    @Override
    public File getYarnPath() {
        if (yarnExecutablePath != null) {
            return yarnExecutablePath;
        }
        String yarnExecutable = getPlatform().isWindows() ? YARN_WINDOWS : YARN_DEFAULT;
        return new File(installConfig.getInstallDirectory() + yarnExecutable);
    }

    @Override
    public File getWorkingDirectory() {
        return installConfig.getWorkingDirectory();
    }

    @Override
    public Platform getPlatform() {
        return installConfig.getPlatform();
    }
}