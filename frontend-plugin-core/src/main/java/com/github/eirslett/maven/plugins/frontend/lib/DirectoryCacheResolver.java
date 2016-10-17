package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public class DirectoryCacheResolver implements CacheResolver {

  private final File cacheDirectory;

  public DirectoryCacheResolver(File cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  @Override
  public File resolve(CacheDescriptor cacheDescriptor) {
    if (!cacheDirectory.exists()) {
      cacheDirectory.mkdirs();
    }

    StringBuilder filename = new StringBuilder()
        .append(cacheDescriptor.getName())
        .append("-")
        .append(cacheDescriptor.getVersion());
    if (cacheDescriptor.getClassifier() != null) {
      filename.append("-").append(cacheDescriptor.getClassifier());
    }
    filename.append(".").append(cacheDescriptor.getExtension());
    return new File(cacheDirectory, filename.toString());
  }

}
