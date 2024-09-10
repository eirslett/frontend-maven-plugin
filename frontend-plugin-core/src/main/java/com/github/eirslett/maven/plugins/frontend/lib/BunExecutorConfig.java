package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface BunExecutorConfig {

    File getNodePath();

    File getBunPath();

    File getWorkingDirectory();

    Platform getPlatform();
}

final class InstallBunExecutorConfig implements BunExecutorConfig {

    public static final String BUN_WINDOWS = BunInstaller.INSTALL_PATH.replaceAll("/", "\\\\") + "\\bun.exe";
    public static final String BUN_DEFAULT = BunInstaller.INSTALL_PATH + "/bun";

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
        String bunExecutable = getPlatform().isWindows() ? BUN_WINDOWS : BUN_DEFAULT;
        return new File(installConfig.getInstallDirectory() + bunExecutable);
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