package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public interface CacheResolver {
  File resolve(CacheDescriptor cacheDescriptor);
}
