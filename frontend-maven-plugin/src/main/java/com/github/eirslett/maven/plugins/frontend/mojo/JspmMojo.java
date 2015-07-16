package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name="jspm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class JspmMojo extends AbstractFrontendMojo {

    /**
     * JSPM arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.jspm", defaultValue = "false")
    private Boolean skip;

    @Override
    protected boolean isSkipped() {
        return this.skip;
    }

    @Override
    protected void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        factory.getJspmRunner().execute(arguments);
    }

}
