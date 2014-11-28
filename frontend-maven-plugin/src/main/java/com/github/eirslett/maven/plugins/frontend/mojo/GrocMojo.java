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

/**
 * Created by Malone on 7/7/14.
 */
@Mojo(name="groc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class GrocMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * Groc arguments. Default is empty (runs just the "groc" command).
     */
    @Parameter(property = "arguments")
    private String arguments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            MojoUtils.setSLF4jLogger(getLog());
            new FrontendPluginFactory(workingDirectory).getGrocRunner()
                    .execute(arguments);
        } catch (TaskRunnerException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }
}