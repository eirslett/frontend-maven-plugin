package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.Goal.JSPM;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.incrementExecutionCount;

@Mojo(name="jspm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class JspmMojo extends AbstractFrontendMojo {

    /**
     * JSPM arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.jspm", defaultValue = "${skip.jspm}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    protected synchronized void execute(FrontendPluginFactory factory) throws Exception {
        incrementExecutionCount(project.getArtifactId(), arguments, JSPM, getFrontendMavenPluginVersion(), false, false, () -> {
        factory.getJspmRunner().execute(arguments, environmentVariables);
        });
    }

}
