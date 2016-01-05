package com.github.eirslett.maven.plugins.frontend.lib;

public class CacheDescriptor {

  private final String name;
  private final String version;
  private final String classifier;
  private final String extension;

  public CacheDescriptor(String name, String version, String extension) {
    this(name, version, null, extension);
  }

  public CacheDescriptor(String name, String version, String classifier, String extension) {
    this.name = name;
    this.version = version;
    this.classifier = classifier;
    this.extension = extension;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getExtension() {
    return extension;
  }
}
