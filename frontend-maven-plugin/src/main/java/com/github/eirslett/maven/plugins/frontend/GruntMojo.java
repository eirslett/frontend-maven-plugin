package com.github.eirslett.maven.plugins.frontend;

import com.google.common.base.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name="grunt", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class GruntMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    @Parameter
    private String target;

    private final Log logger;

    public GruntMojo(){
        logger = super.getLog();
    }

    public GruntMojo(File workingDirectory, String target, Log logger){
        this.workingDirectory = workingDirectory;
        this.target = target;
        this.logger = logger;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logger.info("Running Grunt in "+workingDirectory.toString());
        final String gruntPath = workingDirectory+"/node_modules/grunt-cli/bin/grunt";
        final String gruntCommand = (target == null || target.equals("null") || target.isEmpty())?
                gruntPath : gruntPath + " " + target;
        int result = new NodeExecutor(workingDirectory, getLog()).execute(gruntCommand);
        if(result != 0){
            throw new MojoFailureException("Grunt build failed.");
        }
    }
}