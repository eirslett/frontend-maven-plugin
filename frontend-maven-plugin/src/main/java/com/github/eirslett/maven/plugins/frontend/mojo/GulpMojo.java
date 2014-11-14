package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="gulp", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class GulpMojo extends AbstractNodeJSMojo {

    /**
     * Gulp arguments. Default is empty (runs just the "gulp" command).
     */
    @Parameter(property = "arguments")
    private String arguments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            MojoUtils.setSLF4jLogger(getLog());
            new FrontendPluginFactory(workingDirectory, useGlobal).getGulpRunner()
                    .execute(arguments);
        } catch (TaskRunnerException e) {
            throw new MojoFailureException("Failed to run task", e);
        }
    }
}