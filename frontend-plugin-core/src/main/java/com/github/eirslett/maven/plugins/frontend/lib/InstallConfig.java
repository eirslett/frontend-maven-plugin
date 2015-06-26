package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface InstallConfig {
  File getInstallDirectory();
  File getWorkingDirectory();
  Platform getPlatform();
}

final class DefaultInstallConfig implements InstallConfig {

  private final File installDirectory;
  private final File workingDirectory;
  private final Platform platform;

  public DefaultInstallConfig(File installDirectory,
                              File workingDirectory,
                              Platform platform) {
    this.installDirectory = installDirectory;
    this.workingDirectory = workingDirectory;
    this.platform = platform;
  }

  @Override
  public File getInstallDirectory() {
    return this.installDirectory;
  }

  @Override
  public File getWorkingDirectory() {
    return this.workingDirectory;
  }

  @Override
  public Platform getPlatform() {
    return this.platform;
  }
}