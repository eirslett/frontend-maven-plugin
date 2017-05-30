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
        YarnInstaller.INSTALL_PATH.concat("/dist/bin/yarn.cmd").replaceAll("/", "\\\\");

    private static final String YARN_DEFAULT = YarnInstaller.INSTALL_PATH + "/dist/bin/yarn";

    private File nodePath;

    private final File yarnPath;

    private final InstallConfig installConfig;

    public InstallYarnExecutorConfig(InstallConfig installConfig) {
        this(installConfig, null);
    }

    public InstallYarnExecutorConfig(InstallConfig installConfig, File yarnPath) {
        this.installConfig = installConfig;
        this.yarnPath = yarnPath;
        nodePath = new InstallNodeExecutorConfig(installConfig).getNodePath();
    }

    @Override
    public File getNodePath() {
        return nodePath;
    }

    @Override
    public File getYarnPath() {
        if (yarnPath != null) {
            return yarnPath ;
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