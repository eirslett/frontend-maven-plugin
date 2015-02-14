package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;

@Mojo(name = "ember-build", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class EmberBuildMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(property = "workingDirectory", defaultValue = "${basedir}", required = false)
    private File workingDirectory;

    /**
     * The ember-cli application environment which is used for the build. Defaults to "production".
     */
    @Parameter(property = "environment", defaultValue = "production", required = true)
    private String environment;

    /**
     * The directory where ember-cli places the generated project files. This option
     * defaults to "dist/" in your {@link EmberBuildMojo#workingDirectory}.
     */
    @Parameter(property = "outputPath", defaultValue = "dist/", required = true)
    private File outputPath;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.ember", defaultValue = "false")
    private Boolean skip;

    @Component
    private BuildContext buildContext;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping ember execution");
        } else {
            try {
                MojoUtils.setSLF4jLogger(getLog());
                new FrontendPluginFactory(workingDirectory)
                        .getEmberRunner()
                        .execute(getArguments());
            } catch (TaskRunnerException e) {
                throw new MojoFailureException("Failed to run task", e);
            }

            buildContext.refresh(outputPath);
        }
    }

    private String getArguments() {
        return "build" +
                " --output-path=" + outputPath +
                " --environment=" + environment;

    }
}