package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface YarnExecutorConfig {

    File getNodePath();

    File getYarnPath();

    File getWorkingDirectory();

    Platform getPlatform();

    boolean isYarnBerry();
}

final class InstallYarnExecutorConfig implements YarnExecutorConfig {

    private static final String YARN_WINDOWS =
        YarnInstaller.INSTALL_PATH.concat("/dist/bin/yarn.cmd").replaceAll("/", "\\\\");

    private static final String YARN_DEFAULT = YarnInstaller.INSTALL_PATH + "/dist/bin/yarn";

    private final File nodePath;

    private final InstallConfig installConfig;

    private final boolean isYarnBerry;

    public InstallYarnExecutorConfig(InstallConfig installConfig, final boolean isYarnBerry) {
        this.installConfig = installConfig;
        nodePath = new InstallNodeExecutorConfig(installConfig).getNodePath();
        this.isYarnBerry = isYarnBerry;
    }

    @Override
    public File getNodePath() {
        return nodePath;
    }

    @Override
    public File getYarnPath() {
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

    @Override
    public boolean isYarnBerry() {
        return isYarnBerry;
    }
}