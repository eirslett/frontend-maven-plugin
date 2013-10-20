package com.github.eirslett.maven.plugins.frontend;

import java.io.File;
import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name="karma",  defaultPhase = LifecyclePhase.TEST)
public final class KarmaRunMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * Path to your karma configuration file, relative to the working directory (defaults to "karma.conf.js")
     */
    @Parameter(defaultValue = "karma.conf.js")
    private String karmaConfPath;

    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    private Boolean skipTests;

    private final Log logger;

    public KarmaRunMojo() {
        logger = super.getLog();
    }

    KarmaRunMojo(File workingDirectory, String karmaConfPath, Log logger){
        this.workingDirectory = workingDirectory;
        this.karmaConfPath = karmaConfPath;
        this.logger = logger;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(skipTests){
            logger.info("Skipping karma tests.");
        } else {
            logger.info("Running karma in " + workingDirectory.toString());
            final String karmaPath = workingDirectory+"/node_modules/karma/bin/karma".replace("/", File.separator);
            final String absoluteKarmaConfPath = workingDirectory + File.separator + karmaConfPath;
            int result = new NodeExecutor(workingDirectory, Arrays.asList(karmaPath, "start", absoluteKarmaConfPath)).executeAndRedirectOutput(logger);
            if(result != 0){
                throw new MojoFailureException("Karma run failed.");
            }
        }
    }
}
