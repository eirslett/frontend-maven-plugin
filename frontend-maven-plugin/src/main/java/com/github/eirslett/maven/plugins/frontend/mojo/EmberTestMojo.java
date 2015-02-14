package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "ember-test", defaultPhase = LifecyclePhase.TEST)
public final class EmberTestMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(property = "workingDirectory", defaultValue = "${basedir}", required = false)
    private File workingDirectory;

    /**
     * The ember-cli application environment which is used for the build. Defaults to "test".
     */
    @Parameter(property = "environment", defaultValue = "test", required = true)
    private String environment;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    private Boolean skipTests;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipTests) {
            getLog().info("Skipping ember test execution");
        } else {
            try {
                MojoUtils.setSLF4jLogger(getLog());
                new FrontendPluginFactory(workingDirectory)
                        .getEmberRunner()
                        .execute(getArguments());
            } catch (TaskRunnerException e) {
                throw new MojoFailureException("Failed to run task", e);
            }

        }
    }

    private String getArguments() {
        return "test" +
                " --environment=" + environment;

    }
}