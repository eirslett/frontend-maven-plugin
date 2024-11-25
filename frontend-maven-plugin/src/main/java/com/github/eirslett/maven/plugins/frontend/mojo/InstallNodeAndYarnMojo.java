package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.ArchiveExtractionException;
import com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsInstallationWork;
import com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.Timer;
import com.github.eirslett.maven.plugins.frontend.lib.DownloadException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
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

import java.util.HashMap;
import java.util.Map;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsInstallationWork.UNKNOWN;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.formatNodeVersionForMetric;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.getHostForMetric;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller.ATLASSIAN_NODE_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller.NODEJS_ORG;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.getNodeVersion;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.getDownloadableVersion;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.isBlank;
import static com.github.eirslett.maven.plugins.frontend.lib.YarnInstaller.ATLASSIAN_YARN_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.lib.YarnInstaller.DEFAULT_YARN_DOWNLOAD_ROOT;
import static com.github.eirslett.maven.plugins.frontend.mojo.AtlassianUtil.isAtlassianProject;
import static com.github.eirslett.maven.plugins.frontend.mojo.YarnUtils.isYarnrcYamlFilePresent;
import static java.util.Objects.isNull;

@Mojo(name = "install-node-and-yarn", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallNodeAndYarnMojo extends AbstractFrontendMojo {

    private static final String YARNRC_YAML_FILE_NAME = ".yarnrc.yml";

    /**
     * Where to download Node.js binary from. Defaults to https://nodejs.org/dist/
     */
    @Parameter(property = "nodeDownloadRoot", required = false)
    private String nodeDownloadRoot;

    /**
     * Where to download Yarn binary from. Defaults to https://github.com/yarnpkg/yarn/releases/download/...
     */
    @Parameter(property = "yarnDownloadRoot", required = false)
    private String yarnDownloadRoot;

    /**
     * The version of Node.js to install. IMPORTANT! Most Node.js version names start with 'v', for example
     * 'v0.10.18'
     */
    @Parameter(property = "nodeVersion", defaultValue = "", required = false)
    private String nodeVersion;

    /**
     * The path to the file that contains the Node version to use
     */
    @Parameter(property = "nodeVersionFile", defaultValue = "", required = false)
    private String nodeVersionFile;

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

    private AtlassianDevMetricsInstallationWork packageManagerWork = UNKNOWN;
    private AtlassianDevMetricsInstallationWork runtimeWork = UNKNOWN;

    @Override
    public void execute(FrontendPluginFactory factory) throws Exception {
        boolean pacAttemptFailed = false;
        boolean triedToUsePac = false;
        boolean failed = false;
        Timer timer = new Timer();

        String nodeVersion = getNodeVersion(workingDirectory, this.nodeVersion, this.nodeVersionFile, project.getArtifactId(), getFrontendMavenPluginVersion());

        if (isNull(nodeVersion)) {
            throw new LifecycleExecutionException("Node version could not be detected from a file and was not set");
        }

        if (!NodeVersionHelper.validateVersion(nodeVersion)) {
            throw new LifecycleExecutionException("Node version (" + nodeVersion + ") is not valid. If you think it actually is, raise an issue");
        }

        String validNodeVersion = getDownloadableVersion(nodeVersion);

        boolean isYarnYamlFilePresent = isYarnrcYamlFilePresent(this.session, this.workingDirectory);

        try {
            if (isAtlassianProject(project) &&
                isBlank(serverId) &&
                (isBlank(nodeDownloadRoot) || isBlank(yarnDownloadRoot))
            ) { // If they're overridden the settings, they be the boss
                    triedToUsePac = true;

                    getLog().info("Atlassian project detected, going to use the internal mirrors (requires VPN)");

                serverId = "maven-atlassian-com";
                final String userSetYarnDownloadRoot = yarnDownloadRoot;
                if (isBlank(yarnDownloadRoot)) {
                    yarnDownloadRoot = ATLASSIAN_YARN_DOWNLOAD_ROOT;
                }
                final String userSetNodeDownloadRoot = nodeDownloadRoot;
                if (isBlank(nodeDownloadRoot)) {
                    nodeDownloadRoot = ATLASSIAN_NODE_DOWNLOAD_ROOT;
                }

                try {
                    install(factory, validNodeVersion, isYarnYamlFilePresent);
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
                } finally {
                    nodeDownloadRoot = userSetNodeDownloadRoot;
                    yarnDownloadRoot = userSetYarnDownloadRoot;
                    serverId = null;
                }
            }

            install(factory, validNodeVersion, isYarnYamlFilePresent);
        } catch (Exception exception) {
            failed = true;
            throw exception;
        } finally {
            // Please the compiler being effectively final
            boolean finalFailed = failed;
            boolean finalPacAttemptFailed = pacAttemptFailed;
            boolean finalTriedToUsePac = triedToUsePac;
            timer.stop(
                    "runtime.download",
                    project.getArtifactId(),
                    getFrontendMavenPluginVersion(),
                    formatNodeVersionForMetric(validNodeVersion),
                    new HashMap<String, String>() {{
                        put("installation", "yarn");
                        put("installation-work-runtime", runtimeWork.toString());
                        put("installation-work-package-manager", packageManagerWork.toString());
                        put("runtime-host", getHostForMetric(nodeDownloadRoot, NODEJS_ORG, finalTriedToUsePac, finalPacAttemptFailed));
                        put("package-manager-host", getHostForMetric(isYarnYamlFilePresent ? "" : yarnDownloadRoot, isYarnYamlFilePresent ? "" : DEFAULT_YARN_DOWNLOAD_ROOT, finalTriedToUsePac, finalPacAttemptFailed));
                        put("failed", Boolean.toString(finalFailed));
                        put("pac-attempted-failed", Boolean.toString(finalPacAttemptFailed));
                        put("tried-to-use-pac", Boolean.toString(finalTriedToUsePac));
                    }});
        }
    }

    private void install(FrontendPluginFactory factory, String validNodeVersion, boolean isYarnYamlFilePresent) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(this.session, this.decrypter);
        Server server = MojoUtils.decryptServer(this.serverId, this.session, this.decrypter);

        if (null != server) {
            Map<String, String> httpHeaders = getHttpHeaders(server);
            runtimeWork =
            factory.getNodeInstaller(proxyConfig).setNodeDownloadRoot(this.nodeDownloadRoot)
                .setNodeVersion(validNodeVersion).setUserName(server.getUsername())
                .setPassword(server.getPassword()).setHttpHeaders(httpHeaders).install();
            packageManagerWork =
            factory.getYarnInstaller(proxyConfig).setYarnDownloadRoot(this.yarnDownloadRoot)
                .setYarnVersion(this.yarnVersion).setUserName(server.getUsername())
                .setPassword(server.getPassword()).setHttpHeaders(httpHeaders)
                .setIsYarnBerry(isYarnYamlFilePresent).install();
        } else {
            runtimeWork =
            factory.getNodeInstaller(proxyConfig).setNodeDownloadRoot(this.nodeDownloadRoot)
                .setNodeVersion(validNodeVersion).install();
            packageManagerWork =
            factory.getYarnInstaller(proxyConfig).setYarnDownloadRoot(this.yarnDownloadRoot)
                .setYarnVersion(this.yarnVersion).setIsYarnBerry(isYarnYamlFilePresent).install();
        }
    }

}
