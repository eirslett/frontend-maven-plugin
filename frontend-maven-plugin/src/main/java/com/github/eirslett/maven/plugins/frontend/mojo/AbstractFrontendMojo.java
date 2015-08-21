package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class AbstractFrontendMojo extends AbstractMojo {

  @Component
  protected MojoExecution execution;

  /**
   * Whether you should skip while running in the test phase (default is false)
   */
  @Parameter(property = "skipTests", required = false, defaultValue = "false")
  protected Boolean skipTests;

  /**
   * The base directory for running all Node commands. (Usually the directory that contains package.json)
   */
  @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
  protected File workingDirectory;

  /**
   * The base directory for installing node and npm.
   */
  @Parameter(defaultValue = "${basedir}", property = "installDirectory", required = false)
  protected File installDirectory;

  /**
   * Determines if this execution should be skipped.
   */
  private boolean skipTestPhase() {
    return skipTests && isTestingPhase();
  }

  /**
   * Determines if the current execution is during a testing phase (e.g., "test" or "integration-test").
   */
  private boolean isTestingPhase() {
    String phase = execution.getLifecyclePhase();
    return phase.equals("test") || phase.equals("integration-test");
  }

  protected abstract void execute(FrontendPluginFactory factory) throws FrontendException;

  /**
   * Implemented by children to determine if this execution should be skipped.
   */
  protected abstract boolean skipExecution();

  @Override
  public void execute() throws MojoFailureException {
    if (!(skipTestPhase() || skipExecution())) {
      try {
        execute(new FrontendPluginFactory(workingDirectory, installDirectory));
      } catch (TaskRunnerException e) {
        throw new MojoFailureException("Failed to run task", e);
      } catch (FrontendException e) {
        throw MojoUtils.toMojoFailureException(e);
      }
    } else {
      LoggerFactory.getLogger(AbstractFrontendMojo.class).info("Skipping test phase.");
    }
  }
}
