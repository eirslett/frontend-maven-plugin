package com.github.eirslett.maven.plugins.frontend;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

import static com.github.eirslett.maven.plugins.frontend.MojoUtils.getSlf4jLogger;

@Mojo(name="npm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "arguments", required = false)
    private String arguments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Logger logger = getSlf4jLogger(getLog(), NpmMojo.class);
            logger.info("new NpmRunner(logger, "+Platform.guess().toString()+", "+workingDirectory+").execute("+arguments+")");
            new NpmRunner(logger, Platform.guess(), workingDirectory).execute(arguments);
        } catch (TaskRunnerException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }
}
