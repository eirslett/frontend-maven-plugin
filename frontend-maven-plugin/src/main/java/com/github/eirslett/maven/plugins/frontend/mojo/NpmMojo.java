package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;

@Mojo(name="npm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmMojo extends AbstractFrontendMojo {

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.npm.arguments", required = false)
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
    @Parameter(property = "skip.npm", defaultValue = "false")
    private Boolean skip;

    @Override
    protected boolean isSkipped() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        File packageJson = new File(workingDirectory, "package.json");
        if (buildContext == null || buildContext.hasDelta(packageJson) || !buildContext.isIncremental()) {
            ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session, decrypter);
            factory.getNpmRunner(proxyConfig).execute(arguments);
        } else {
            getLog().info("Skipping npm install as package.json unchanged");
        }
    }
}
