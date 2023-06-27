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

import java.util.Collections;

@Mojo(name = "bower", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class BowerMojo extends AbstractFrontendMojo {

    /**
     * Bower arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.bower", defaultValue = "${skip.bower}")
    private boolean skip;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(property = "frontend.bower.bowerInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean bowerInheritsProxyConfigFromMaven;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    protected synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        ProxyConfig proxyConfig = getProxyConfig();
        factory.getBowerRunner(proxyConfig).execute(arguments, environmentVariables);
    }

    private ProxyConfig getProxyConfig() {
        if (bowerInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(session, decrypter);
        } else {
            getLog().info("bower not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        }
    }

}
