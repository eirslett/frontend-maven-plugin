package com.github.eirslett.maven.plugins.frontend.mojo;

import java.util.Collections;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

@Mojo(name="node",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NodeMojo extends AbstractFrontendMojo {

	/**
     * node javscript target file.
     */
    @Parameter(property = "frontend.node.task", required = true)
    private String jsTarget;
    
    /**
     * node arguments. Default is "-v" for version.
     */
    @Parameter(defaultValue = "-v", property = "frontend.node.arguments", required = false)
    private String arguments;

    @Parameter(property = "frontend.node.nodeInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean nodeInheritsProxyConfigFromMaven;
    
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private BuildContext buildContext;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.node", defaultValue = "${skip.node}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        ProxyConfig proxyConfig = getProxyConfig();
        factory.getNodeRunner(jsTarget, proxyConfig).execute(arguments, environmentVariables);
    }

    private ProxyConfig getProxyConfig() {
        if (nodeInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(session, decrypter);
        } else {
            getLog().info("node not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        }
    }

}
