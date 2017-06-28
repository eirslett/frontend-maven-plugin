package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.ArrayList;
import java.util.List;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

public interface YarnRunner extends NodeTaskRunner {
}

final class DefaultYarnRunner extends YarnTaskExecutor implements YarnRunner {

    private static final String TASK_NAME = "yarn";

    public DefaultYarnRunner(YarnExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, config.getYarnPath().getAbsolutePath(),
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
}
