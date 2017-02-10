package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

public abstract class AbstractFrontendMojo extends AbstractMojo {

  @Component
  protected MojoExecution execution;

  /**
   * Whether you should skip while running in the test phase (default is false)
   */
  @Parameter(property = "skipTests", required = false, defaultValue = "false")
  protected Boolean skipTests;

  /**
   * Set this to "true" to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
   * occasion.
   */
  @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
  protected Boolean testFailureIgnore;

  /**
   * The base directory for running all Node commands. (Usually the directory that contains package.json)
   */
  @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
  protected File workingDirectory;

  /**
   * The base directory for installing node and npm.
   */
  @Parameter(property = "installDirectory", required = false)
  protected File installDirectory;


  /**
   * Additional environment variables to pass to the build.
   */
   @Parameter
   protected Map<String, String> environmentVariables;

  @Parameter(
      defaultValue = "${project}",
      readonly = true
  )
  private MavenProject project;

  @Parameter(
      defaultValue = "${repositorySystemSession}",
      readonly = true
  )
  private RepositorySystemSession repositorySystemSession;

  /**
   * Determines if this execution should be skipped.
   */
  private boolean skipTestPhase() {
    return skipTests && isTestingPhase();
  }

  private boolean ignoreTestFailure() {
    return testFailureIgnore && isTestingPhase();
  }
  
  /**
   * Determines if the current execution is during a testing phase (e.g., "test" or "integration-test").
   */
  private boolean isTestingPhase() {
    String phase = execution.getLifecyclePhase();
    return phase!=null && (phase.equals("test") || phase.equals("integration-test"));
  }

  protected abstract void execute(FrontendPluginFactory factory) throws FrontendException;

  /**
   * Implemented by children to determine if this execution should be skipped.
   */
  protected abstract boolean skipExecution();

  @Override
  public void execute() throws MojoFailureException {
    if (!(skipTestPhase() || skipExecution())) {
      if (installDirectory == null) {
        installDirectory = workingDirectory;
      }
      try {
        execute(new FrontendPluginFactory(
            workingDirectory,
            installDirectory,
            new RepositoryCacheResolver(repositorySystemSession)
        ));
      } catch (TaskRunnerException e) {
        if (!ignoreTestFailure()) {
          throw new MojoFailureException("Failed to run task", e);
        } else {
          getLog().error("Failed to run task", e);
        }
      } catch (FrontendException e) {
        MojoFailureException mojoFailed = MojoUtils.toMojoFailureException(e);
        if (!ignoreTestFailure()) {
          throw mojoFailed;
        } else {
          getLog().error(mojoFailed);
        }
      }
    } else {
      LoggerFactory.getLogger(AbstractFrontendMojo.class).info("Skipping test phase.");
    }
  }
}
