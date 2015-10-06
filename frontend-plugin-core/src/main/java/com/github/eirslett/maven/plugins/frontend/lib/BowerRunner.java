package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.ArrayList;
import java.util.List;

public interface BowerRunner extends NodeTaskRunner {}

final class DefaultBowerRunner extends NodeTaskExecutor implements BowerRunner {

    private static final String TASK_LOCATION = "node_modules/bower/bin/bower";

    DefaultBowerRunner(NodeExecutorConfig config, ProxyConfig proxyConfig) {
        super(config, TASK_LOCATION, buildArguments(proxyConfig));
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig) {
        List<String> arguments = new ArrayList<String>();

        if (!proxyConfig.isEmpty()) {
            ProxyConfig.Proxy secureProxy = proxyConfig.getSecureProxy();
            if (secureProxy != null){
                arguments.add("--config.https-proxy=" + secureProxy.getUri().toString());
            }

            ProxyConfig.Proxy insecureProxy = proxyConfig.getInsecureProxy();
            if (insecureProxy != null) {
                arguments.add("--config.proxy=" + insecureProxy.getUri().toString());
            }
        }
        return arguments;
    }
}
