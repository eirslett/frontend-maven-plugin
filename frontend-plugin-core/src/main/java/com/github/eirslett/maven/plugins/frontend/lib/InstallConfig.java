package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface InstallConfig {
  File getInstallDirectory();
  File getWorkingDirectory();
  CacheResolver getCacheResolver();
  Platform getPlatform();
  boolean isUseSystemNode();
}

final class DefaultInstallConfig implements InstallConfig {

  private final File installDirectory;
  private final File workingDirectory;
  private final CacheResolver cacheResolver;
  private final Platform platform;
  private final boolean useSystemNode;
  
  public DefaultInstallConfig(File installDirectory,
                              File workingDirectory,
                              CacheResolver cacheResolver,
                              Platform platform,
                              boolean useSystemNode) {
    this.installDirectory = installDirectory;
    this.workingDirectory = workingDirectory;
    this.cacheResolver = cacheResolver;
    this.platform = platform;
    this.useSystemNode = useSystemNode;
  }

  @Override
  public File getInstallDirectory() {
    return this.installDirectory;
  }

  @Override
  public File getWorkingDirectory() {
    return this.workingDirectory;
  }
  
  public CacheResolver getCacheResolver() {
    return cacheResolver;
  }

  @Override
  public Platform getPlatform() {
    return this.platform;
  }

  @Override
  public boolean isUseSystemNode() {
    return useSystemNode;
  }
}
