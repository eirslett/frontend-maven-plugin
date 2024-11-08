package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.ArchiveExtractionException;
import com.github.eirslett.maven.plugins.frontend.lib.CorepackInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.DownloadException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector;
import com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.util.Map;

import static com.github.eirslett.maven.plugins.frontend.lib.CorepackInstaller.ATLASSIAN_COREPACK_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.CorepackInstaller.DEFAULT_COREPACK_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller.ATLASSIAN_NODE_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.getDownloadableVersion;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.isBlank;
import static com.github.eirslett.maven.plugins.frontend.mojo.AtlassianUtil.isAtlassianProject;
import static java.util.Objects.isNull;

@Mojo(name="install-node-and-corepack", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallNodeAndCorepackMojo extends AbstractFrontendMojo {

    /**
     * Where to download Node.js binary from. Defaults to https://nodejs.org/dist/
     */
    @Parameter(property = "nodeDownloadRoot", required = false)
    private String nodeDownloadRoot;

    /**
     * Where to download corepack binary from. Defaults to https://registry.npmjs.org/corepack/-/
     */
    @Parameter(property = "corepackDownloadRoot", required = false)
    private String corepackDownloadRoot;

    /**
     * The version of Node.js to install. IMPORTANT! Most Node.js version names start with 'v', for example 'v0.10.18'
     */
    @Parameter(property="nodeVersion", required = false)
    private String nodeVersion;

    /**
     * The path to the file that contains the Node version to use
     */
    @Parameter(property = "nodeVersionFile", defaultValue = "", required = false)
    private String nodeVersionFile;

    /**
     * The version of corepack to install. Note that the version string can optionally be prefixed with
     * 'v' (i.e., both 'v1.2.3' and '1.2.3' are valid).
     *
     * If not provided, then the corepack version bundled with Node will be used.
     */
    @Parameter(property = "corepackVersion", required = false, defaultValue = "provided")
    private String corepackVersion;

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
    @Parameter(property = "skip.installnodecorepack", defaultValue = "${skip.installnodecorepack}")
    private boolean skip;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws Exception {
        String nodeVersion = NodeVersionDetector.getNodeVersion(workingDirectory, this.nodeVersion, this.nodeVersionFile);

        if (isNull(nodeVersion)) {
            throw new LifecycleExecutionException("Node version could not be detected from a file and was not set");
        }

        if (!NodeVersionHelper.validateVersion(nodeVersion)) {
            throw new LifecycleExecutionException("Node version (" + nodeVersion + ") is not valid. If you think it actually is, raise an issue");
        }

        String validNodeVersion = getDownloadableVersion(nodeVersion);

        if (isAtlassianProject(project) && isBlank(serverId) &&
                (isBlank(nodeDownloadRoot) || isBlank(corepackDownloadRoot))
        ) { // If they're overridden the settings, they be the boss
            getLog().info("Atlassian project detected, going to use the internal mirrors (requires VPN)");

            serverId = "maven-atlassian-com";
            try {
                install(factory, validNodeVersion,
                        isBlank(nodeDownloadRoot) ? ATLASSIAN_NODE_DOWNLOAD_ROOT : nodeDownloadRoot,
                        isBlank(corepackDownloadRoot) ? ATLASSIAN_COREPACK_DOWNLOAD_ROOT : corepackDownloadRoot);
                return;
            } catch (InstallationException exception) {
                // Ignore as many filesystem exceptions unrelated to the mirror easily
                if (!(exception.getCause() instanceof DownloadException ||
                        exception.getCause() instanceof ArchiveExtractionException)) {
                    throw exception;
                }
                getLog().warn("Oh no couldn't use the internal mirrors! Falling back to upstream mirrors");
                getLog().debug("Using internal mirrors failed because: ", exception);
            } finally {
                serverId = null;
            }
        }

        String resolvedNodeDownloadRoot = getNodeDownloadRoot();
        String resolvedCorepackDownloadRoot = getCorepackDownloadRoot();
        if (isBlank(resolvedCorepackDownloadRoot)) {
            resolvedCorepackDownloadRoot = DEFAULT_COREPACK_DOWNLOAD_ROOT;
        }

        install(factory, validNodeVersion, resolvedNodeDownloadRoot, resolvedCorepackDownloadRoot);
    }

    private void install(FrontendPluginFactory factory, String validNodeVersion, String resolvedNodeDownloadRoot, String resolvedCorepackDownloadRoot) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session, decrypter);

        // Setup the installers
        NodeInstaller nodeInstaller = factory.getNodeInstaller(proxyConfig);
        nodeInstaller.setNodeVersion(validNodeVersion)
                .setNodeDownloadRoot(resolvedNodeDownloadRoot);
        if ("provided".equals(corepackVersion)) {
            // This causes the node installer to copy over the whole
            // node_modules directory including the corepack module
            nodeInstaller.setNpmVersion("provided");
        }
        CorepackInstaller corepackInstaller = factory.getCorepackInstaller(proxyConfig);
        corepackInstaller.setCorepackVersion(corepackVersion)
                .setCorepackDownloadRoot(resolvedCorepackDownloadRoot);

        // If pplicable, configure authentication details
        Server server = MojoUtils.decryptServer(serverId, session, decrypter);
        if (null != server) {
            Map<String, String> httpHeaders = getHttpHeaders(server);
            nodeInstaller
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .setHttpHeaders(httpHeaders);
            corepackInstaller
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .setHttpHeaders(httpHeaders);
        }

        // Perform the installation
        nodeInstaller.install();
        corepackInstaller.install();
    }

    private String getNodeDownloadRoot() {
        return nodeDownloadRoot;
    }

    private String getCorepackDownloadRoot() {
        return corepackDownloadRoot;
    }
}
