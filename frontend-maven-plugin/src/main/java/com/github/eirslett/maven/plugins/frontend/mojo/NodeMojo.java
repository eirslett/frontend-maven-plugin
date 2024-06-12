package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="node",  defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class NodeMojo extends AbstractFrontendMojo {
    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "", property = "frontend.node.arguments", required = false)
    private String arguments;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.node", defaultValue = "${skip.node}")
    private boolean skip;

    @Override
    protected void execute(FrontendPluginFactory factory) throws FrontendException {
        getLog().info("Running NodeMojo");
        factory.getNodeRunner().execute(arguments, environmentVariables);
    }

    @Override
    protected boolean skipExecution() {
        return skip;
    }
}
