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
import java.util.LinkedList;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.Utils.normalize;

@Mojo(name="grunt", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class GruntMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    @Parameter
    private String arguments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log logger = getLog();
        logger.info("Running Grunt in "+workingDirectory.toString());
        final String gruntPath = workingDirectory + normalize("/node_modules/grunt-cli/bin/grunt");

	    List<String> commands =  new LinkedList(Arrays.asList(gruntPath));
	    if(arguments != null && !arguments.equals("null") && !arguments.isEmpty()) {
		    commands.addAll(Arrays.asList(arguments.split("\\s+")));
	    }

        int result = new NodeExecutor(workingDirectory, commands).executeAndRedirectOutput(logger);
        if(result != 0){
            throw new MojoFailureException("Grunt build failed.");
        }
    }
}