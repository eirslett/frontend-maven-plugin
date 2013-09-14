package com.github.eirslett.maven.plugins.frontend;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.github.eirslett.maven.plugins.frontend.Utils.executeAndRedirectOutput;

@Mojo(name="npm-install",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmInstallMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private java.io.File workingDirectory;

    private final Log logger;

    public NpmInstallMojo() {
        logger = super.getLog();
    }

    public NpmInstallMojo(File workingDirectory, Log logger){
        this.workingDirectory = workingDirectory;
        this.logger = logger;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Running NPM install in "+workingDirectory.toString());
        final String npmPath = "node/npm/bin/npm-cli.js".replace("/", File.separator);
        int result = new NodeExecutor(workingDirectory, logger).execute(npmPath+" install");

    }
}
