package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.PreExecutionException;
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
import java.util.Collections;

@Mojo(name="npx",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class NpxMojo extends AbstractFrontendMojo {

    private static final String NPM_REGISTRY_URL = "npmRegistryURL";
    
    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.npx.arguments", required = false)
    private String arguments;

    @Parameter(property = "frontend.npx.npmInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean npmInheritsProxyConfigFromMaven;

    /**
     * Registry override, passed as the registry option during npm install if set.
     */
    @Parameter(property = NPM_REGISTRY_URL, required = false, defaultValue = "")
    private String npmRegistryURL;
    
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private BuildContext buildContext;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.npx", defaultValue = "${skip.npx}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException, PreExecutionException {
        File packageJson = new File(workingDirectory, "package.json");
        if (buildContext == null || buildContext.hasDelta(packageJson) || !buildContext.isIncremental()) {
            ProxyConfig proxyConfig = getProxyConfig();
            factory.getNpxRunner(proxyConfig, getRegistryUrl()).execute(arguments, getEnvironmentVariables());
        } else {
            getLog().info("Skipping npm install as package.json unchanged");
        }
    }

    private ProxyConfig getProxyConfig() {
        if (npmInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(session, decrypter);
        } else {
            getLog().info("npm not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        }
    }

    private String getRegistryUrl() {
        // check to see if overridden via `-D`, otherwise fallback to pom value
        return System.getProperty(NPM_REGISTRY_URL, npmRegistryURL);
    }
}
