package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;

@Mojo(name = "install-bun", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallBunMojo extends AbstractFrontendMojo {

    /**
     * The version of Bun to install. IMPORTANT! Most Bun version names start with 'v', for example
     * 'v1.0.0'
     */
    @Parameter(property = "bunVersion", required = true)
    private String bunVersion;

    /**
     * Server Id for download username and password
     */
    @Parameter(property = "serverId", defaultValue = "")
    private String serverId;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.installbun", alias = "skip.installbun", defaultValue = "${skip.installbun}")
    private boolean skip;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(this.session, this.decrypter);
        Server server = MojoUtils.decryptServer(this.serverId, this.session, this.decrypter);
        if (null != server) {
            factory.getBunInstaller(proxyConfig).setBunVersion(this.bunVersion).setUserName(server.getUsername())
                    .setPassword(server.getPassword()).setHttpHeaders(getHttpHeaders(server)).install();
        } else {
            factory.getBunInstaller(proxyConfig).setBunVersion(this.bunVersion).install();
        }
    }

}
