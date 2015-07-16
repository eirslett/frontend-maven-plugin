package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public abstract class AbstractFrontendMojo extends AbstractMojo {

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

  protected abstract void execute(FrontendPluginFactory factory) throws FrontendException;

  protected abstract boolean isSkipped();

  @Override
  public void execute() throws MojoFailureException {
    if (!isSkipped()) {
      try {
        execute(new FrontendPluginFactory(workingDirectory, installDirectory));
      } catch (TaskRunnerException e) {
        throw new MojoFailureException("Failed to run task", e);
      } catch (FrontendException e) {
        throw MojoUtils.toMojoFailureException(e);
      }
    }
  }
}
