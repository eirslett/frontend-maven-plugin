package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Frontend maven plugin for Mojos.
 */
public abstract class FrontendMavenMojo extends AbstractMojo {

    /**
     * Skip the executions.
     * @parameter expression="${skip.frontend}"
     * default-value="false"
     * @since 0.17
     */
    @Parameter(defaultValue = "false", property = "skip", required = false)
    private boolean skip;

    public boolean isSkip() {
        return skip;
    }
}
