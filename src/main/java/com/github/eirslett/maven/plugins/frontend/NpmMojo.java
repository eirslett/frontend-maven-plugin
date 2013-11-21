package com.github.eirslett.maven.plugins.frontend;

import java.io.File;
import java.util.Arrays;

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

    @Parameter(defaultValue = "install", property = "command", required = false)
    private String command;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running NPM '" + command + "' in "+workingDirectory.toString());
        final String npmPath = workingDirectory+"/node/npm/bin/npm-cli.js".replace("/", File.separator);
        int result = new NodeExecutor(workingDirectory, Arrays.asList(npmPath, command)).executeAndRedirectOutput(getLog());
        if(result != 0){
            throw new MojoFailureException("NPM '" + command + "' failed.");
        }
    }
}
