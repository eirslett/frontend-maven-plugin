package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public interface NpmRunner extends NodeTaskRunner {}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";

    public DefaultNpmRunner(final NodeExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return config.getNpmPath().call().getAbsolutePath();
            }
        }, buildArguments(proxyConfig, npmRegistryURL));
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig, String npmRegistryURL) {
        List<String> arguments = new ArrayList<String>();
               
        if (npmRegistryURL != null)
        {
            arguments.add ("--registry=" + npmRegistryURL);
        }

        if (!proxyConfig.isEmpty()) {
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
