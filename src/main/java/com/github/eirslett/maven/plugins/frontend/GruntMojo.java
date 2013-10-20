package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Mojo(name="grunt", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class GruntMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    @Parameter
    private String target;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log logger = getLog();
        logger.info("Running Grunt in "+workingDirectory.toString());
        final String gruntPath = workingDirectory+"/node_modules/grunt-cli/bin/grunt".replace("/", File.separator);

        List<String> commands;
        if(target == null || target.equals("null") || target.isEmpty()) {
            commands = Arrays.asList(gruntPath);
        } else {
            commands = Arrays.asList(gruntPath, target);
        }

        int result = new NodeExecutor(workingDirectory, commands).executeAndRedirectOutput(logger);
        if(result != 0){
            throw new MojoFailureException("Grunt build failed.");
        }
    }
}