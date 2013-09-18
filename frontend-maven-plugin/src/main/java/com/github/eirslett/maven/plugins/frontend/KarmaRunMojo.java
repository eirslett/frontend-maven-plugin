package com.github.eirslett.maven.plugins.frontend;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.github.eirslett.maven.plugins.frontend.Utils.executeAndGetResult;
import static com.github.eirslett.maven.plugins.frontend.Utils.joinPath;

@Mojo(name="karma",  defaultPhase = LifecyclePhase.TEST)
public final class KarmaRunMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    @Parameter(defaultValue = "karma.conf.js")
    private String karmaConfPath;

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
        logger.info("Running karma in " + workingDirectory.toString());
        final String karmaPath = workingDirectory+"/node_modules/karma/bin/karma".replace("/", File.separator);
        int result = new NodeExecutor(workingDirectory, getLog()).execute(karmaPath, "start", workingDirectory+File.separator+karmaConfPath);
        if(result != 0){
            throw new MojoFailureException("Karma run failed.");
        }
    }
}
