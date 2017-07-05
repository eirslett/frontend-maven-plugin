package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NPMInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.Server;

@Mojo(name="install-node-and-npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallNodeAndNpmMojo extends AbstractFrontendMojo {

    /**
     * Where to download Node.js binary from. Defaults to http://nodejs.org/dist/
     */
    @Parameter(property = "nodeDownloadRoot", required = false, defaultValue = NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
    private String nodeDownloadRoot;

    /**
     * Where to download NPM binary from. Defaults to http://registry.npmjs.org/npm/-/
     */
    @Parameter(property = "npmDownloadRoot", required = false, defaultValue = NPMInstaller.DEFAULT_NPM_DOWNLOAD_ROOT)
    private String npmDownloadRoot;

    /**
     * Where to download Node.js and NPM binaries from.
     *
     * @deprecated use {@link #nodeDownloadRoot} and {@link #npmDownloadRoot} instead, this configuration will be used only when no {@link #nodeDownloadRoot} or {@link #npmDownloadRoot} is specified.
     */
    @Parameter(property = "downloadRoot", required = false, defaultValue = "")
    @Deprecated
    private String downloadRoot;

    /**
     * The version of Node.js to install. IMPORTANT! Most Node.js version names start with 'v', for example 'v0.10.18'
     */
    @Parameter(property="nodeVersion", required = true)
    private String nodeVersion;

    /**
     * The version of NPM to install.
     */
    @Parameter(property = "npmVersion", required = false, defaultValue = "provided")
    private String npmVersion;

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
    @Parameter(property = "skip.installnodenpm", defaultValue = "${skip.installnodenpm}")
    private boolean skip;

    /**
     * allows setting of http proxy overrides so that settings.xml does not need to be modified
     * @since 1.5
     */
    @Parameter
    private ProxyOverrideConfig httpProxyOverride;

    /**
     * allows setting of https proxy overrides so that settings.xml does not need to be modified
     * @since 1.5
     */
    @Parameter
    private ProxyOverrideConfig httpsProxyOverride;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(httpProxyOverride, httpsProxyOverride, session, decrypter);
        String nodeDownloadRoot = getNodeDownloadRoot();
        String npmDownloadRoot = getNpmDownloadRoot();
        Server server = MojoUtils.decryptServer(serverId, session, decrypter);
        if (null != server) {
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(nodeVersion)
                .setNodeDownloadRoot(nodeDownloadRoot)
                .setNpmVersion(npmVersion)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .install();
            factory.getNPMInstaller(proxyConfig)
                .setNodeVersion(nodeVersion)
                .setNpmVersion(npmVersion)
                .setNpmDownloadRoot(npmDownloadRoot)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .install();
        } else {
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(nodeVersion)
                .setNodeDownloadRoot(nodeDownloadRoot)
                .setNpmVersion(npmVersion)
                .install();
            factory.getNPMInstaller(proxyConfig)
                .setNodeVersion(this.nodeVersion)
                .setNpmVersion(this.npmVersion)
                .setNpmDownloadRoot(npmDownloadRoot)
                .install();
        }
    }

    private String getNodeDownloadRoot() {
        if (downloadRoot != null && !"".equals(downloadRoot) && NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT.equals(nodeDownloadRoot)) {
            return downloadRoot;
        }
        return nodeDownloadRoot;
    }

    private String getNpmDownloadRoot() {
        if (downloadRoot != null && !"".equals(downloadRoot) && NPMInstaller.DEFAULT_NPM_DOWNLOAD_ROOT.equals(npmDownloadRoot)) {
            return downloadRoot;
        }
        return npmDownloadRoot;
    }
}
