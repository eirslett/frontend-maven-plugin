package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="install-node-and-npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class InstallNodeAndNpmMojo extends AbstractMojo {

    @Parameter(defaultValue = "v0.10.18")
    private String nodeVersion;

    @Parameter(defaultValue = "1.3.9")
    private String npmVersion;

    @Parameter(defaultValue = "${basedir}")
    private String targetDir;

    public InstallNodeAndNpmMojo(){

    }

    public InstallNodeAndNpmMojo(String nodeVersion, String npmVersion, String targetDir){
        this.nodeVersion = nodeVersion;
        this.npmVersion = npmVersion;
        this.targetDir = targetDir;
    }


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        new NodeAndNPMInstaller(nodeVersion, npmVersion, targetDir, getLog()).install();
    }
}
