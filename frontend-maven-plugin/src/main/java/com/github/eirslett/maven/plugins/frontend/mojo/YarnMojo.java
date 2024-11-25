package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.ExecutionCoordinates;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalMojoHelper;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import com.github.eirslett.maven.plugins.frontend.lib.YarnRunner;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.github.eirslett.maven.plugins.frontend.mojo.YarnUtils.isYarnrcYamlFilePresent;

@Mojo(name = "yarn", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class YarnMojo extends AbstractFrontendMojo {

    private static final String NPM_REGISTRY_URL = "npmRegistryURL";

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "", property = "frontend.yarn.arguments", required = false)
    private String arguments;

    /**
     * Enable or disable incremental builds, on by default
     */
    @Parameter(defaultValue = "true", property = "frontend.incremental", alias = "incrementalbuild.enabled", required = false)
    private String incremental;

    @Parameter(property = "frontend.yarn.yarnInheritsProxyConfigFromMaven", required = false,
        defaultValue = "true")
    private boolean yarnInheritsProxyConfigFromMaven;

    /**
     * Registry override, passed as the registry option during npm install if set.
     */
    @Parameter(property = NPM_REGISTRY_URL, required = false, defaultValue = "")
    private String npmRegistryURL;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * Files that should be checked for changes for incremental builds in addition
     * to the defaults in {@link IncrementalMojoHelper}. Directories will be searched.
     */
    @Parameter(property = "triggerFiles", alias = "trigger.files", required = false)
    private Set<File> triggerFiles;

    /**
     * Files that should NOT be checked for changes for incremental builds in addition
     * to the defaults in {@link IncrementalMojoHelper}. Whole directories will be
     * excluded.
     */
    @Parameter(property = "excludedFilenames", alias = "excluded.filenames", required = false, defaultValue = "build,target,dist")
    private Set<String> excludedFilenames;

    @Component
    private BuildContext buildContext;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.yarn", defaultValue = "${skip.yarn}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        IncrementalMojoHelper incrementalHelper = new IncrementalMojoHelper(incremental, getTargetDir(), workingDirectory, triggerFiles, excludedFilenames);

        ProxyConfig proxyConfig = getProxyConfig();
        boolean isYarnBerry = isYarnrcYamlFilePresent(this.session, this.workingDirectory);
        YarnRunner runner = factory.getYarnRunner(proxyConfig, getRegistryUrl(), isYarnBerry);

        ExecutionCoordinates coordinates = new ExecutionCoordinates(execution.getGoal(), execution.getExecutionId(), execution.getLifecyclePhase());
        if (incrementalHelper.shouldExecute(arguments, coordinates, runner.getRuntime(), environmentVariables)) {
            File packageJson = new File(this.workingDirectory, "package.json");
            if (this.buildContext == null || this.buildContext.hasDelta(packageJson)
                    || !this.buildContext.isIncremental()) {
                runner.execute(this.arguments,
                        this.environmentVariables);

                incrementalHelper.acceptIncrementalBuildDigest();
            } else {
                getLog().info("Skipping yarn install as package.json unchanged");
            }
        } else {
            getLog().info("Skipping yarn execution as no modified files in" + workingDirectory);
        }
    }

    private ProxyConfig getProxyConfig() {
        if (this.yarnInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(this.session, this.decrypter);
        } else {
            getLog().info("yarn not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        }
    }

    private String getRegistryUrl() {
        // check to see if overridden via `-D`, otherwise fallback to pom value
        return System.getProperty(NPM_REGISTRY_URL, this.npmRegistryURL);
    }
}
