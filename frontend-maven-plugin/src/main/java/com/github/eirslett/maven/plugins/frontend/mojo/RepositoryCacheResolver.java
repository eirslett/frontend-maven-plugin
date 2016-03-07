package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.CacheDescriptor;
import com.github.eirslett.maven.plugins.frontend.lib.CacheResolver;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;

import java.io.File;

public class RepositoryCacheResolver implements CacheResolver {

  private static final String GROUP_ID = "com.github.eirslett";
  private final RepositorySystemSession repositorySystemSession;

  public RepositoryCacheResolver(RepositorySystemSession repositorySystemSession) {
    this.repositorySystemSession = repositorySystemSession;
  }

  @Override
  public File resolve(CacheDescriptor cacheDescriptor) {
    LocalRepositoryManager manager = repositorySystemSession.getLocalRepositoryManager();
    File localArtifact = new File(
        manager.getRepository().getBasedir(),
        manager.getPathForLocalArtifact(createArtifact(cacheDescriptor))
    );
    return localArtifact;
  }

  private DefaultArtifact createArtifact(CacheDescriptor cacheDescriptor) {
    String version = cacheDescriptor.getVersion().replaceAll("^v", "");

    DefaultArtifact artifact;

    if (cacheDescriptor.getClassifier() == null) {
      artifact = new DefaultArtifact(
          GROUP_ID,
          cacheDescriptor.getName(),
          cacheDescriptor.getExtension(),
          version
      );
    } else {
      artifact = new DefaultArtifact(
          GROUP_ID,
          cacheDescriptor.getName(),
          cacheDescriptor.getClassifier(),
          cacheDescriptor.getExtension(),
          version
      );
    }
    return artifact;
  }
}
