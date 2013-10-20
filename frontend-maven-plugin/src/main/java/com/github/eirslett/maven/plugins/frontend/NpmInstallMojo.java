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

@Mojo(name="npm-install",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmInstallMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    private final Log logger;

    public NpmInstallMojo() {
        logger = super.getLog();
    }

    NpmInstallMojo(File workingDirectory, Log logger){
        this.workingDirectory = workingDirectory;
        this.logger = logger;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running NPM install in "+workingDirectory.toString());
        final String npmPath = workingDirectory+"/node/npm/bin/npm-cli.js".replace("/", File.separator);
        int result = new NodeExecutor(workingDirectory, Arrays.asList(npmPath, "install")).executeAndRedirectOutput(logger);
        if(result != 0){
            throw new MojoFailureException("NPM install failed.");
        }
    }
}
