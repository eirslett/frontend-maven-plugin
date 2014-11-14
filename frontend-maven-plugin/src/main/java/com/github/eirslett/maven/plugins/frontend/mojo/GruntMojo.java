package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="grunt", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class GruntMojo extends AbstractNodeJSMojo {

    /**
     * Grunt arguments. Default is empty (runs just the "grunt" command).
     */
    @Parameter(property = "arguments")
    private String arguments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            MojoUtils.setSLF4jLogger(getLog());
            new FrontendPluginFactory(workingDirectory, useGlobal).getGruntRunner()
                    .execute(arguments);
        } catch (TaskRunnerException e) {
            throw new MojoFailureException("Failed to run task", e);
        }
    }
}