package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "bower", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class BowerMojo extends AbstractFrontendMojo {

    /**
     * Bower arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.bower", defaultValue = "false")
    private Boolean skip;

    @Override
    protected boolean isSkipped() {
        return this.skip;
    }

    @Override
    protected void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        factory.getBowerRunner().execute(arguments);
    }
}
