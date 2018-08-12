package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;
import java.util.Collections;

import com.github.eirslett.maven.plugins.frontend.lib.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "yarn", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class YarnMojo extends AbstractFrontendMojo {

    private static final String NPM_REGISTRY_URL = "npmRegistryURL";

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "", property = "frontend.yarn.arguments", required = false)
    private String arguments;

    @Parameter(property = "frontend.yarn.yarnInheritsProxyConfigFromMaven", required = false,
        defaultValue = "true")
    private boolean yarnInheritsProxyConfigFromMaven;

    /**
     * Registry override, passed as the registry option during npm install if set.
     */
    @Parameter(property = NPM_REGISTRY_URL, required = false, defaultValue = "")
    private String npmRegistryURL;

    /**
     * Server Id for access to npm registry
     */
    @Parameter(property = "npmRegistryServerId", defaultValue = "")
    private String npmRegistryServerId;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

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
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        File packageJson = new File(this.workingDirectory, "package.json");
        if (this.buildContext == null || this.buildContext.hasDelta(packageJson)
            || !this.buildContext.isIncremental()) {
            ProxyConfig proxyConfig = getProxyConfig();
            NpmRegistryConfig registryConfig = getRegistryConfig();
            factory.getYarnRunner(proxyConfig, registryConfig).execute(this.arguments,
                this.environmentVariables);
        } else {
            getLog().info("Skipping yarn install as package.json unchanged");
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

    private NpmRegistryConfig getRegistryConfig() {
        // check to see if overridden via `-D`, otherwise fallback to pom value
        final String registryURL = System.getProperty(NPM_REGISTRY_URL, npmRegistryURL);
        if (null == registryURL || registryURL.isEmpty()) {
            return null;
        }

        String username = null;
        String password = null;
        if (null != npmRegistryServerId && !npmRegistryServerId.isEmpty()) {
            Server server = MojoUtils.decryptServer(npmRegistryServerId, session, decrypter);
            if (null != server) {
                username = server.getUsername();
                password = server.getPassword();
            }
        }
        return new NpmRegistryConfig(registryURL, username, password);
    }
}
