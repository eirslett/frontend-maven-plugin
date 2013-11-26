package com.github.eirslett.maven.plugins.frontend;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="npm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    @Parameter(defaultValue = "install", property = "arguments", required = false)
    private String arguments;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running 'npm " + arguments + "' in "+workingDirectory.toString());
        final String npmPath = workingDirectory + normalize("/node/npm/bin/npm-cli.js");

        List<String> commands = new LinkedList<String>(Arrays.asList(npmPath));
        if(arguments != null && !arguments.equals("null") && !arguments.isEmpty()) {
            commands.addAll(Arrays.asList(arguments.split("\\s+")));
        }

        int result = new NodeExecutor(workingDirectory, commands).executeAndRedirectOutput(getLog());
        if(result != 0){
            throw new MojoFailureException("'npm "+arguments+"' failed.");
        }
    }
}
