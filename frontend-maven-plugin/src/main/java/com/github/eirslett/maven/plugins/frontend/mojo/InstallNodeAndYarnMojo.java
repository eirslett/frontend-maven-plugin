package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.YarnInstaller;

@Mojo(name = "install-node-and-yarn", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallNodeAndYarnMojo extends AbstractFrontendMojo {

    /**
     * Where to download Node.js binary from. Defaults to http://nodejs.org/dist/
     */
    @Parameter(property = "nodeDownloadRoot", required = false,
        defaultValue = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
    private String nodeDownloadRoot;

    /**
     * Where to download Yarn binary from. Defaults to https://github.com/yarnpkg/yarn/releases/download/...
     */
    @Parameter(property = "yarnDownloadRoot", required = false,
        defaultValue = YarnInstaller.DEFAULT_YARN_DOWNLOAD_ROOT)
    private String yarnDownloadRoot;

    /**
     * The version of Node.js to install. IMPORTANT! Most Node.js version names start with 'v', for example
     * 'v0.10.18'
     */
    @Parameter(property = "nodeVersion", required = true)
    private String nodeVersion;

    /**
     * The version of Yarn to install. IMPORTANT! Most Yarn names start with 'v', for example 'v0.15.0'.
     */
    @Parameter(property = "yarnVersion", required = true)
    private String yarnVersion;

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
    @Parameter(property = "skip.installyarn", alias = "skip.installyarn", defaultValue = "${skip.installyarn}")
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
            factory.getNodeInstaller(proxyConfig).setNodeDownloadRoot(this.nodeDownloadRoot)
                .setNodeVersion(this.nodeVersion).setPassword(server.getPassword())
                .setUserName(server.getUsername()).install();
            factory.getYarnInstaller(proxyConfig).setYarnDownloadRoot(this.yarnDownloadRoot)
                .setYarnVersion(this.yarnVersion).setUserName(server.getUsername())
                .setPassword(server.getPassword()).install();
        } else {
            factory.getNodeInstaller(proxyConfig).setNodeDownloadRoot(this.nodeDownloadRoot)
                .setNodeVersion(this.nodeVersion).install();
            factory.getYarnInstaller(proxyConfig).setYarnDownloadRoot(this.yarnDownloadRoot)
                .setYarnVersion(this.yarnVersion).install();
        }
    }

}
