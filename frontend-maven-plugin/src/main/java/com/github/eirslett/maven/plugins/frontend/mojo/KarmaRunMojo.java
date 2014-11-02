package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.LoggerFactory;


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
    @Parameter(defaultValue = "karma.conf.js", property = "karmaConfPath")
    private String karmaConfPath;

    /**
     * Whether you should skip running the tests (default is false)
     */
    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    private Boolean skipTests;

    /**
     * Whether you should continue build when some test will fail (default is false)
     */
    @Parameter(property = "testFailureIgnore", required = false, defaultValue = "false")
    private Boolean testFailureIgnore;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            MojoUtils.setSLF4jLogger(getLog());
            if(skipTests){
                LoggerFactory.getLogger(KarmaRunMojo.class).info("Skipping karma tests.");
            } else {
                new FrontendPluginFactory(workingDirectory).getKarmaRunner()
                        .execute("start " + karmaConfPath);
            }
        } catch (TaskRunnerException e) {
            if (testFailureIgnore) {
                LoggerFactory.getLogger(KarmaRunMojo.class).warn("There are ignored test failures/errors for: " + workingDirectory);
            } else {
                throw new MojoFailureException("Failed to run task", e);
            }
        }
    }
}
