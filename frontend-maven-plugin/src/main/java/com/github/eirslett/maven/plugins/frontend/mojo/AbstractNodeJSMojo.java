package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractNodeJSMojo extends AbstractMojo {

	/**
	 * Should this command use a local installed instance of npm (in the system PATH).
	 * Else the instance installed by install-npm is used.
	 */
	@Parameter(property = "npm.useGlobal", defaultValue = "false", required = false)
	protected boolean useGlobal;

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    protected File workingDirectory;
	
}