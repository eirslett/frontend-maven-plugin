package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;

public interface NpmRunner extends NodeTaskRunner {}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";

    public DefaultNpmRunner(NodeExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, config.getNpmPath().getAbsolutePath(), buildArguments(proxyConfig, npmRegistryURL));
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig, String npmRegistryURL) {
        List<String> arguments = new ArrayList<String>();

        if (npmRegistryURL != null) {
            arguments.add("--registry=" + npmRegistryURL);
        }

        if (!proxyConfig.isEmpty()) {
            if (npmRegistryURL != null) {
                Proxy registryProxy = proxyConfig.getProxyForUrl(npmRegistryURL);
                addProxyToArguments(registryProxy, arguments);
            } else {
                addProxyToArguments(proxyConfig.getSecureProxy(), arguments);
                addProxyToArguments(proxyConfig.getInsecureProxy(), arguments);
            }
        }

        return arguments;
    }

    private static void addProxyToArguments(Proxy proxy, List<String> arguments) {
        if (proxy != null) {
            arguments.add((proxy.isSecure() ? "--https-proxy=" : "--proxy=") + proxy.getUri().toString());
        }
    }
}
