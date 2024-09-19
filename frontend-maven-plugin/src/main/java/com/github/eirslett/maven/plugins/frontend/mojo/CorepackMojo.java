package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

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
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        File packageJson = new File(workingDirectory, "package.json");
        if (buildContext == null || buildContext.hasDelta(packageJson) || !buildContext.isIncremental()) {
            factory.getCorepackRunner().execute(arguments, environmentVariables);
        } else {
            getLog().info("Skipping corepack install as package.json unchanged");
        }
    }
}
