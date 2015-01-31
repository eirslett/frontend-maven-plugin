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

@Mojo(name = "bower", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class BowerMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * Bower arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.bower", defaultValue = "false")
    private Boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(!skip) {
            try {
                MojoUtils.setSLF4jLogger(getLog());
                new FrontendPluginFactory(workingDirectory).getBowerRunner().execute(arguments);
            } catch (TaskRunnerException e) {
                throw new MojoFailureException("Failed to run task", e);
            }
        }
    }
}
