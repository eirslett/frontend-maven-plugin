package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface NodeExecutorConfig {
  File getNodePath();
  File getNpmPath();
  File getWorkingDirectory();
  Platform getPlatform();
}

final class InstallNodeExecutorConfig implements NodeExecutorConfig {

  private static final String NODE_WINDOWS = "\\node\\node.exe";
  private static final String NODE_DEFAULT = "/node/node";
  private static final String NPM = "/node/npm/bin/npm-cli.js";

  private final InstallConfig installConfig;

  public InstallNodeExecutorConfig(InstallConfig installConfig) {
    this.installConfig = installConfig;
  }

  @Override
  public File getNodePath() {
    String nodeExecutable = getPlatform().isWindows() ? NODE_WINDOWS : NODE_DEFAULT;
    return new File(installConfig.getInstallDirectory() + nodeExecutable);
  }

  @Override
  public File getNpmPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(NPM));
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