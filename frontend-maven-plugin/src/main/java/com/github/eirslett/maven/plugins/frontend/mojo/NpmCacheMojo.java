package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.NpmRunner;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import java.util.HashMap;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


/**
 * Mojo for the npm-cache build. It will use a special {@link NpmRunner} to call the "npm-cache" command instead of the
 * "npm" one.
 *
 * @author dtuerk
 */
@Mojo(name = "npm-cache", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmCacheMojo extends AbstractNpmMojo {

    private static final String INSTALL_NPM_CACHE = "install -g npm-cache";

    protected NpmRunner getNpmRunner(FrontendPluginFactory factory, ProxyConfig proxyConfig) {
        return factory.getNpmCacheRunner(proxyConfig, getRegistryUrl());
    }

    @Override
    protected void executeNpm(FrontendPluginFactory factory, ProxyConfig proxyConfig) throws TaskRunnerException {
        if (!factory.isNpmCacheInstalled()) {
            getLog().info("No npm-cache found! Trying to install it local...");
            try {
                factory.getNpmRunner(proxyConfig, getRegistryUrl())
                        .execute(INSTALL_NPM_CACHE, new HashMap<String, String>());
                getLog().info("... npm-cache installed!");
            } catch (TaskRunnerException e) {
                getLog().error("Can't install npm-cache!", e);
            }
        }
        super.executeNpm(factory, proxyConfig);
    }
}
