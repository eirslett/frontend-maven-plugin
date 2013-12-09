package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

import java.io.File;

@Mojo(name="install-node-and-npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class InstallNodeAndNpmMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(property = "workingDirectory", defaultValue = "${basedir}")
    private File workingDirectory;

    /**
     * The version of Node.js to install. IMPORTANT! Most Node.js version names start with 'v', for example 'v0.10.18'
     */
    @Parameter(property="nodeVersion", required = true)
    private String nodeVersion;

    /**
     * The version of NPM to install.
     */
    @Parameter(property = "npmVersion", required = true)
    private String npmVersion;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Logger logger = MojoUtils.getSlf4jLogger(getLog(), InstallNodeAndNpmMojo.class);
            FrontendPluginFactory.getNodeAndNPMInstaller(workingDirectory, logger).install(nodeVersion, npmVersion);
        } catch (InstallationException e) {
            throw MojoUtils.toMojoFailureException(e);
        }
    }
}
