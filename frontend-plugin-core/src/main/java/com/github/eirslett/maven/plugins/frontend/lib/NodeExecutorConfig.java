package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface NodeExecutorConfig {
  File getNodePath();
  File getNpmPath();
  File getPnpmPath();
  File getPnpmCjsPath();

  File getNpxPath();
  File getInstallDirectory();
  File getWorkingDirectory();
  Platform getPlatform();
}

final class InstallNodeExecutorConfig implements NodeExecutorConfig {

  private static final String NODE_WINDOWS = NodeInstaller.INSTALL_PATH.replaceAll("/", "\\\\") + "\\node.exe";
  private static final String NODE_DEFAULT = NodeInstaller.INSTALL_PATH + "/node";
  private static final String NPM = NodeInstaller.INSTALL_PATH + "/node_modules/npm/bin/npm-cli.js";
  private static final String PNPM = NodeInstaller.INSTALL_PATH + "/node_modules/pnpm/bin/pnpm.js";
  private static final String PNPM_CJS = NodeInstaller.INSTALL_PATH + "/node_modules/pnpm/bin/pnpm.cjs";
  private static final String NPX = NodeInstaller.INSTALL_PATH + "/node_modules/npm/bin/npx-cli.js";

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
  public File getPnpmPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(PNPM));
  }

  @Override
  public File getPnpmCjsPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(PNPM_CJS));
  }

  @Override
  public File getNpxPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(NPX));
  }

  @Override
  public File getInstallDirectory() {
    return installConfig.getInstallDirectory();
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
