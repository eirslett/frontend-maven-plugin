package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

import java.io.File;

import static com.github.eirslett.maven.plugins.frontend.MojoUtils.getSlf4jLogger;
import static com.github.eirslett.maven.plugins.frontend.MojoUtils.toMojoFailureException;

@Mojo(name="install-node-and-npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class InstallNodeAndNpmMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}")
    private File workingDirectory;

    /**
     * The version of Node.js to install. IMPORTANT! Most Node.js version names start with 'v', for example 'v0.10.18'
     */
    @Parameter(required = true)
    private String nodeVersion;

    /**
     * The version of NPM to install.
     */
    @Parameter(required = true)
    private String npmVersion;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Logger logger = getSlf4jLogger(getLog(), InstallNodeAndNpmMojo.class);
            new DefaultNodeAndNPMInstaller(workingDirectory, logger, Platform.guess(), new DefaultArchiveExtractor(), new DefaultFileDownloader()).install(nodeVersion, npmVersion);
        } catch (InstallationException e) {
            throw toMojoFailureException(e);
        }
    }
}
