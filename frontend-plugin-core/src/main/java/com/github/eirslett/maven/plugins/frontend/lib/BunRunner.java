package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;

public interface BunRunner extends NodeTaskRunner {
}

final class DefaultBunRunner extends BunTaskExecutor implements BunRunner {

    private static final String TASK_NAME = "bun";

    public DefaultBunRunner(BunExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, config.getBunPath().getAbsolutePath(),
                buildArguments(proxyConfig, npmRegistryURL));
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig, String npmRegistryURL) {
        List<String> arguments = new ArrayList<>();

        if (npmRegistryURL != null && !npmRegistryURL.isEmpty()) {
            arguments.add("--registry=" + npmRegistryURL);
        }

        if (!proxyConfig.isEmpty()) {
            Proxy proxy = null;
            if (npmRegistryURL != null && !npmRegistryURL.isEmpty()) {
                proxy = proxyConfig.getProxyForUrl(npmRegistryURL);
            }

            if (proxy == null) {
                proxy = proxyConfig.getSecureProxy();
            }

            if (proxy == null) {
                proxy = proxyConfig.getInsecureProxy();
            }

            arguments.add("--https-proxy=" + proxy.getUri().toString());
            arguments.add("--proxy=" + proxy.getUri().toString());
        }

        return arguments;
    }
    public Optional<Runtime> getRuntime() {
        try {
            String version = new BunExecutor(config, singletonList(" --version"), emptyMap())
                    .executeAndGetResult(logger);
            return Optional.of(new Runtime("bun", version));
        } catch (Exception exception) {
            logger.debug("Failed to get Bun version, because: ", exception);
            return empty();
        }
    }
}
