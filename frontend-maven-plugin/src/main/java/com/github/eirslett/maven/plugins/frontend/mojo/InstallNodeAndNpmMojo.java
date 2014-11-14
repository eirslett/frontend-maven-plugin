package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeAndNPMInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;

@Mojo(name="install-node-and-npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class InstallNodeAndNpmMojo extends AbstractNodeJSMojo {

    /**
     * Where to download Node.js binary from. Defaults to http://nodejs.org/dist/
     */
    @Parameter(property = "nodeDownloadRoot", required = false, defaultValue = NodeAndNPMInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
    private String nodeDownloadRoot;

    /**
     * Where to download NPM binary from. Defaults to http://registry.npmjs.org/npm/-/
     */
    @Parameter(property = "npmDownloadRoot", required = false, defaultValue = NodeAndNPMInstaller.DEFAULT_NPM_DOWNLOAD_ROOT)
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
    @Parameter(property = "npmVersion", required = true)
    private String npmVersion;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            MojoUtils.setSLF4jLogger(getLog());
            
            if (useGlobal) {
            	getLog().warn("preinstalled mode is set, skipping downloading of node an npm.");
            	return;
            }
            
            ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session);
            String nodeDownloadRoot = getNodeDownloadRoot();
            String npmDownloadRoot = getNpmDownloadRoot();
            new FrontendPluginFactory(workingDirectory, proxyConfig, useGlobal).getNodeAndNPMInstaller().install(nodeVersion, npmVersion, nodeDownloadRoot, npmDownloadRoot);
        } catch (InstallationException e) {
            throw MojoUtils.toMojoFailureException(e);
        }
    }

    private String getNodeDownloadRoot() {
        if (downloadRoot != null && !"".equals(downloadRoot) && NodeAndNPMInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT.equals(nodeDownloadRoot)) {
            return downloadRoot;
        }
        return nodeDownloadRoot;
    }

    private String getNpmDownloadRoot() {
        if (downloadRoot != null && !"".equals(downloadRoot) && NodeAndNPMInstaller.DEFAULT_NPM_DOWNLOAD_ROOT.equals(npmDownloadRoot)) {
            return downloadRoot;
        }
        return npmDownloadRoot;
    }
}
