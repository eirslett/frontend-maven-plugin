package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.Goal.COREPACK;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.incrementExecutionCount;
import static com.github.eirslett.maven.plugins.frontend.mojo.MojoUtils.incrementalBuildEnabled;

@Mojo(name="corepack",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class CorepackMojo extends AbstractFrontendMojo {

    /**
     * corepack arguments. Default is "enable".
     */
    @Parameter(defaultValue = "enable", property = "frontend.corepack.arguments", required = false)
    private String arguments;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private BuildContext buildContext;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.corepack", defaultValue = "${skip.corepack}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws Exception {
        File packageJson = new File(workingDirectory, "package.json");

        boolean incrementalEnabled = incrementalBuildEnabled(buildContext);
        boolean willBeIncremental = incrementalEnabled && buildContext.hasDelta(packageJson);

        incrementExecutionCount(project.getArtifactId(), arguments, COREPACK, getFrontendMavenPluginVersion(), incrementalEnabled, willBeIncremental, () -> {

        if (!willBeIncremental) {
            factory.getCorepackRunner().execute(arguments, environmentVariables);
        } else {
            getLog().info("Skipping corepack install as package.json unchanged");
        }

        });
    }
}
