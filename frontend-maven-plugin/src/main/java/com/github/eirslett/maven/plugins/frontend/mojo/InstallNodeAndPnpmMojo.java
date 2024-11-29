package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.ArchiveExtractionException;
import com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsInstallationWork;
import com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.Timer;
import com.github.eirslett.maven.plugins.frontend.lib.DownloadException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector;
import com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper;
import com.github.eirslett.maven.plugins.frontend.lib.PnpmInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.util.HashMap;
import java.util.Map;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsInstallationWork.UNKNOWN;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.formatNodeVersionForMetric;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.getHostForMetric;
import static com.github.eirslett.maven.plugins.frontend.lib.NPMInstaller.ATLASSIAN_NPM_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller.ATLASSIAN_NODE_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller.NODEJS_ORG;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.getDownloadableVersion;
import static com.github.eirslett.maven.plugins.frontend.lib.PnpmInstaller.DEFAULT_PNPM_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.isBlank;
import static com.github.eirslett.maven.plugins.frontend.mojo.AtlassianUtil.isAtlassianProject;
import static java.util.Objects.isNull;

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
    @Parameter(property = "pnpmDownloadRoot", required = false)
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
    @Parameter(property = "nodeVersion", defaultValue = "", required = false)
    private String nodeVersion;

    /**
     * The path to the file that contains the Node version to use
     */
    @Parameter(property = "nodeVersionFile", defaultValue = "", required = false)
    private String nodeVersionFile;

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

    private AtlassianDevMetricsInstallationWork packageManagerWork = UNKNOWN;
    private AtlassianDevMetricsInstallationWork runtimeWork = UNKNOWN;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws Exception {
        boolean pacAttemptFailed = false;
        boolean triedToUsePac = false;
        boolean failed = false;
        Timer timer = new Timer();

        String nodeVersion = NodeVersionDetector.getNodeVersion(workingDirectory, this.nodeVersion, this.nodeVersionFile, project.getArtifactId(), getFrontendMavenPluginVersion());

        if (isNull(nodeVersion)) {
            throw new LifecycleExecutionException("Node version could not be detected from a file and was not set");
        }

        if (!NodeVersionHelper.validateVersion(nodeVersion)) {
            throw new LifecycleExecutionException("Node version (" + nodeVersion + ") is not valid. If you think it actually is, raise an issue");
        }

        String validNodeVersion = getDownloadableVersion(nodeVersion);

        // Use different names to avoid confusion with fields `nodeDownloadRoot` and
        // `pnpmDownloadRoot`.
        //
        // TODO: Remove the `downloadRoot` config (with breaking change) to simplify download root
        // resolution.
        String resolvedNodeDownloadRoot = getNodeDownloadRoot();
        String resolvedPnpmDownloadRoot = getPnpmDownloadRoot();

        try {
            if (isAtlassianProject(project) &&
                    isBlank(serverId) &&
                    (isBlank(resolvedNodeDownloadRoot) || isBlank(resolvedPnpmDownloadRoot))
            ) { // If they're overridden the settings, they be the boss
                triedToUsePac = true;

                getLog().info("Atlassian project detected, going to use the internal mirrors (requires VPN)");

                serverId = "maven-atlassian-com";
                resolvedNodeDownloadRoot = isBlank(resolvedNodeDownloadRoot) ? ATLASSIAN_NODE_DOWNLOAD_ROOT : resolvedNodeDownloadRoot;
                resolvedPnpmDownloadRoot = isBlank(resolvedPnpmDownloadRoot) ? ATLASSIAN_NPM_DOWNLOAD_ROOT : resolvedPnpmDownloadRoot;
                try {
                    install(factory, validNodeVersion, resolvedNodeDownloadRoot, resolvedPnpmDownloadRoot);
                    return;
                } catch (InstallationException exception) {
                    // Ignore as many filesystem exceptions unrelated to the mirror easily
                    if (!(exception.getCause() instanceof DownloadException ||
                            exception.getCause() instanceof ArchiveExtractionException)) {
                        throw exception;
                    }
                    pacAttemptFailed = true;
                    getLog().warn("Oh no couldn't use the internal mirrors! Falling back to upstream mirrors");
                    getLog().debug("Using internal mirrors failed because: ", exception);

                    resolvedNodeDownloadRoot = getNodeDownloadRoot();
                    resolvedPnpmDownloadRoot = getPnpmDownloadRoot();
                    serverId = null;
                }
            }

            install(factory, validNodeVersion, resolvedNodeDownloadRoot, resolvedPnpmDownloadRoot);
        } catch (Exception exception) {
            failed = true;
            throw exception;
        } finally {
            // Please the compiler being effectively final
            boolean finalFailed = failed;
            boolean finalPacAttemptFailed = pacAttemptFailed;
            boolean finalTriedToUsePac = triedToUsePac;
            String finalResolvedPnpmDownloadRoot = resolvedPnpmDownloadRoot;
            String finalResolvedNodeDownloadRoot = resolvedNodeDownloadRoot;
            timer.stop(
                    "runtime.download",
                    project.getArtifactId(),
                    getFrontendMavenPluginVersion(),
                    formatNodeVersionForMetric(validNodeVersion),
                    new HashMap<String, String>() {{
                        put("installation", "pnpm");
                        put("installation-work-runtime", runtimeWork.toString());
                        put("installation-work-package-manager", packageManagerWork.toString());
                        put("runtime-host", getHostForMetric(finalResolvedNodeDownloadRoot, NODEJS_ORG, finalTriedToUsePac, finalPacAttemptFailed));
                        put("package-manager-host", getHostForMetric(finalResolvedPnpmDownloadRoot, DEFAULT_PNPM_DOWNLOAD_ROOT, finalTriedToUsePac, finalPacAttemptFailed));
                        put("failed", Boolean.toString(finalFailed));
                        put("pac-attempted-failed", Boolean.toString(finalPacAttemptFailed));
                        put("tried-to-use-pac", Boolean.toString(finalTriedToUsePac));
                    }});
        }
    }

    private void install(FrontendPluginFactory factory, String validNodeVersion, String resolvedNodeDownloadRoot, String resolvedPnpmDownloadRoot) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session, decrypter);
        Server server = MojoUtils.decryptServer(serverId, session, decrypter);

        if (null != server) {
            Map<String, String> httpHeaders = getHttpHeaders(server);
            runtimeWork =
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(validNodeVersion)
                .setNodeDownloadRoot(resolvedNodeDownloadRoot)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .setHttpHeaders(httpHeaders)
                .install();
            packageManagerWork =
            factory.getPnpmInstaller(proxyConfig)
                .setPnpmVersion(pnpmVersion)
                .setPnpmDownloadRoot(resolvedPnpmDownloadRoot)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .setHttpHeaders(httpHeaders)
                .install();
        } else {
            runtimeWork =
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(validNodeVersion)
                .setNodeDownloadRoot(resolvedNodeDownloadRoot)
                .install();
            packageManagerWork =
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
        if (downloadRoot != null && !"".equals(downloadRoot) && isBlank(pnpmDownloadRoot)) {
            return downloadRoot;
        }
        return pnpmDownloadRoot;
    }
}
