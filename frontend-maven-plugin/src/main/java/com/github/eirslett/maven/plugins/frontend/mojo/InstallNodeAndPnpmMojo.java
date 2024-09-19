package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.PnpmInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;

import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.Server;

@Mojo(name="install-node-and-pnpm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallNodeAndPnpmMojo extends AbstractFrontendMojo {

    /**
     * Where to download Node.js binary from. Defaults to https://nodejs.org/dist/
     */
    @Parameter(property = "nodeDownloadRoot", required = false)
    private String nodeDownloadRoot;

    /**
     * Where to download pnpm binary from. Defaults to https://registry.npmjs.org/pnpm/-/
     */
    @Parameter(property = "pnpmDownloadRoot", required = false, defaultValue = PnpmInstaller.DEFAULT_PNPM_DOWNLOAD_ROOT)
    private String pnpmDownloadRoot;

    /**
     * Where to download Node.js and pnpm binaries from.
     *
     * @deprecated use {@link #nodeDownloadRoot} and {@link #pnpmDownloadRoot} instead, this configuration will be used only when no {@link #nodeDownloadRoot} or {@link #pnpmDownloadRoot} is specified.
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
     * The version of pnpm to install. Note that the version string can optionally be prefixed with
     * 'v' (i.e., both 'v1.2.3' and '1.2.3' are valid).
     */
    @Parameter(property = "pnpmVersion", required = true)
    private String pnpmVersion;

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
    @Parameter(property = "skip.installnodepnpm", defaultValue = "${skip.installnodepnpm}")
    private boolean skip;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session, decrypter);
        // Use different names to avoid confusion with fields `nodeDownloadRoot` and
        // `pnpmDownloadRoot`.
        //
        // TODO: Remove the `downloadRoot` config (with breaking change) to simplify download root
        // resolution.
        String resolvedNodeDownloadRoot = getNodeDownloadRoot();
        String resolvedPnpmDownloadRoot = getPnpmDownloadRoot();
        Server server = MojoUtils.decryptServer(serverId, session, decrypter);
        if (null != server) {
            Map<String, String> httpHeaders = getHttpHeaders(server);
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(nodeVersion)
                .setNodeDownloadRoot(resolvedNodeDownloadRoot)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .setHttpHeaders(httpHeaders)
                .install();
            factory.getPnpmInstaller(proxyConfig)
                .setPnpmVersion(pnpmVersion)
                .setPnpmDownloadRoot(resolvedPnpmDownloadRoot)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .setHttpHeaders(httpHeaders)
                .install();
        } else {
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(nodeVersion)
                .setNodeDownloadRoot(resolvedNodeDownloadRoot)
                .install();
            factory.getPnpmInstaller(proxyConfig)
                .setPnpmVersion(this.pnpmVersion)
                .setPnpmDownloadRoot(resolvedPnpmDownloadRoot)
                .install();
        }
    }

    private String getNodeDownloadRoot() {
        if (downloadRoot != null && !"".equals(downloadRoot) && nodeDownloadRoot == null) {
            return downloadRoot;
        }
        return nodeDownloadRoot;
    }

    private String getPnpmDownloadRoot() {
        if (downloadRoot != null && !"".equals(downloadRoot) && PnpmInstaller.DEFAULT_PNPM_DOWNLOAD_ROOT.equals(pnpmDownloadRoot)) {
            return downloadRoot;
        }
        return pnpmDownloadRoot;
    }
}
