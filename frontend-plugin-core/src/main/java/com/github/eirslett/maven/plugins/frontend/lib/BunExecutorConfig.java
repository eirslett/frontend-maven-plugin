package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface BunExecutorConfig {

    File getNodePath();

    File getBunPath();

    File getWorkingDirectory();

    Platform getPlatform();
}

final class InstallBunExecutorConfig implements BunExecutorConfig {

    private File nodePath;

    private final InstallConfig installConfig;

    public InstallBunExecutorConfig(InstallConfig installConfig) {
        this.installConfig = installConfig;
        nodePath = new InstallNodeExecutorConfig(installConfig).getNodePath();
    }

    @Override
    public File getNodePath() {
        return nodePath;
    }

    @Override
    public File getBunPath() {
        return new File(installConfig.getInstallDirectory() + BunInstaller.INSTALL_PATH);
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