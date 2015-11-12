package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

@Mojo(name = "grunth", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class GruntHMojo extends AbstractFrontendMojo {
  /**
   * Grunth arguments. Default is empty (runs just the "grunth" command).
   */
  @Parameter(property = "frontend.grunth.arguments")
  private String arguments;

  /**
   * Files that should be checked for changes, in addition to the srcdir files.
   * Defaults to Gruntfile.js in the {@link #workingDirectory}.
   */
  @Parameter(property = "triggerfiles")
  private List<File> triggerfiles;

  /**
   * The directory containing front end files that will be processed by grunt.
   * If this is set then files in the directory will be checked for
   * modifications before running grunth.
   */
  @Parameter(property = "srcdir")
  private File srcdir;

  /**
   * The directory where front end files will be output by grunth. If this is
   * set then they will be refreshed so they correctly show as modified in
   * Eclipse.
   */
  @Parameter(property = "outputdir")
  private File outputdir;

  /**
   * Skips execution of this mojo.
   */
  @Parameter(property = "skip.grunth", defaultValue = "false")
  private Boolean skip;

  @Component
  private BuildContext buildContext;

  @Override
  protected boolean skipExecution() {
    return this.skip;
  }

  @Override
  public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
    if (shouldExecute()) {
      factory.getGruntHRunner().execute(arguments);

      if (outputdir != null) {
        getLog().info("Refreshing files after grunth: " + outputdir);
        buildContext.refresh(outputdir);
      }
    }
    else {
      getLog().info("Skipping grunth as no modified files in " + srcdir);
    }
  }

  private boolean shouldExecute() {
    if (triggerfiles == null || triggerfiles.isEmpty()) {
      triggerfiles = Arrays.asList(new File(workingDirectory, "Gruntfile.js"));
    }

    return MojoUtils.shouldExecute(buildContext, triggerfiles, srcdir);
  }
}
