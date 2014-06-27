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


@Mojo(name="protractor",  defaultPhase = LifecyclePhase.TEST)
public final class ProtractorRunMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * Path to your protractor configuration file, relative to the working directory (default is "protractor.conf.js")
     */
    @Parameter(defaultValue = "protractor.conf.js", property = "protractorConfPath")
    private String protractorConfPath;

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
                LoggerFactory.getLogger(ProtractorRunMojo.class).info("Skipping protractor tests.");
            } else {
                new FrontendPluginFactory(workingDirectory).getProtractorRunner()
                        .execute("start " + protractorConfPath);
            }
        } catch (TaskRunnerException e) {
            if (testFailureIgnore) {
                LoggerFactory.getLogger(ProtractorRunMojo.class).warn("There are ignored test failures/errors for: " + workingDirectory);
            } else {
                throw new MojoFailureException(e.getMessage());
            }
        }
    }
}
