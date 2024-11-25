package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.Goal.KARMA;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.incrementExecutionCount;


@Mojo(name="karma",  defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public final class KarmaRunMojo extends AbstractFrontendMojo {

    /**
     * Path to your karma configuration file, relative to the working directory (default is "karma.conf.js")
     */
    @Parameter(defaultValue = "karma.conf.js", property = "karmaConfPath")
    private String karmaConfPath;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.karma", defaultValue = "${skip.karma}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws Exception {
        incrementExecutionCount(project.getArtifactId(), karmaConfPath, KARMA, getFrontendMavenPluginVersion(), false, false, () -> {
        factory.getKarmaRunner().execute("start " + karmaConfPath, environmentVariables);
        });
    }
}
