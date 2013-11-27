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


@Mojo(name="karma",  defaultPhase = LifecyclePhase.TEST)
public final class KarmaRunMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * Path to your karma configuration file, relative to the working directory (default is "karma.conf.js")
     */
    @Parameter(defaultValue = "karma.conf.js")
    private String karmaConfPath;

    /**
     * Whether you should skip running the tests (default is false)
     */
    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    private Boolean skipTests;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Logger logger = getSlf4jLogger(getLog(), KarmaRunMojo.class);
            if(skipTests){
                logger.info("Skipping karma tests.");
            } else {
                new KarmaRunner(logger, Platform.guess(), workingDirectory).execute("start "+karmaConfPath);
            }
        } catch (TaskRunnerException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }
}
