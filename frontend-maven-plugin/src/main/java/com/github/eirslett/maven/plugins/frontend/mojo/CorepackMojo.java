package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.CorepackRunner;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalMojoHelper;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.ExecutionCoordinates;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Set;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.Goal.COREPACK;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.incrementExecutionCount;

@Mojo(name="corepack",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class CorepackMojo extends AbstractFrontendMojo {

    /**
     * corepack arguments. Default is "enable".
     */
    @Parameter(defaultValue = "enable", property = "frontend.corepack.arguments", required = false)
    private String arguments;

    /**
     * Enable or disable incremental builds, on by default
     */
    @Parameter(defaultValue = "true", property = "frontend.incremental", required = false)
    private String frontendIncremental;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private BuildContext buildContext;

    /**
     * Files that should be checked for changes for incremental builds in addition
     * to the defaults in {@link IncrementalMojoHelper}. Directories will be searched.
     */
    @Parameter(property = "triggerFiles", required = false)
    private Set<File> triggerFiles;

    /**
     * Files that should NOT be checked for changes for incremental builds in addition
     * to the defaults in {@link IncrementalMojoHelper}. Whole directories will be
     * excluded.
     */
    @Parameter(property = "excludedFilenames", required = false, defaultValue = "node_modules,lcov-report,coverage,screenshots,build,dist,target,.idea,.history,tmp,.settings,.vscode,.git,dependency-reduced-pom.xml,.flattened-pom.xml")
    private Set<String> excludedFilenames;

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
        CorepackRunner runner = factory.getCorepackRunner();

        ExecutionCoordinates coordinates = new ExecutionCoordinates(execution.getGoal(), execution.getExecutionId(), execution.getLifecyclePhase());
        IncrementalMojoHelper incrementalHelper = new IncrementalMojoHelper(frontendIncremental, coordinates, getTargetDir(), workingDirectory, triggerFiles, excludedFilenames);

        boolean incrementalEnabled = incrementalHelper.incrementalEnabled();
        boolean isIncremental = incrementalEnabled && incrementalHelper.canBeSkipped(arguments, runner.getRuntime(), environmentVariables, project.getArtifactId(), getFrontendMavenPluginVersion());

        incrementExecutionCount(project.getArtifactId(), arguments, COREPACK, getFrontendMavenPluginVersion(), incrementalEnabled, isIncremental, () -> {
            if (isIncremental) {
                getLog().info("Skipping corepack execution as no modified files in " + workingDirectory);
            } else {
                runner.execute(arguments, environmentVariables);

                incrementalHelper.acceptIncrementalBuildDigest();
            }
        });
    }
}
