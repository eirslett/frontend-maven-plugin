package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;

public interface NpmRunner {
    void execute(String args) throws TaskRunnerException;
}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";

    public DefaultNpmRunner(NodeExecutorConfig config, ProxyConfig proxyConfig, String npmRegistry) {
        super(config, TASK_NAME, config.getNpmPath().getAbsolutePath(), buildArguments(proxyConfig, npmRegistry));
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig, String npmRegistry) {
        List<String> arguments = new ArrayList<String>();
        arguments.add("--color=false");

        arguments.add("--registry=" + npmRegistry);

        if (!proxyConfig.isEmpty() && proxyConfig.getProxyForUrl(npmRegistry) != null) {
            Proxy secureProxy = proxyConfig.getSecureProxy();
            if (secureProxy != null){
                arguments.add("--https-proxy=" + secureProxy.getUri().toString());
            }

            Proxy insecureProxy = proxyConfig.getInsecureProxy();
            if (insecureProxy != null) {
                arguments.add("--proxy=" + insecureProxy.getUri().toString());
            }
        }
        return arguments;
    }
}
