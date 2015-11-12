package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.Arrays;

public interface GruntHRunner {
  void execute(String args) throws TaskRunnerException;
}

final class DefaultGruntHRunner extends NodeTaskExecutor implements GruntHRunner {
  private static final String TASK_LOCATION = "node_modules/grunth-cli/bin/grunth";

  DefaultGruntHRunner(NodeExecutorConfig config) {
    super(config, TASK_LOCATION, Arrays.asList("--no-color"));
  }
}
