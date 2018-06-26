package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;

public interface NodeRunner extends NodeTaskRunner {}

final class DefaultNodeRunner extends NodeTaskExecutor implements NodeRunner {

    public DefaultNodeRunner(String jsTarget, NodeExecutorConfig config, ProxyConfig proxyConfig) {
        super(config, config.getGlobalPath(jsTarget).getAbsolutePath(), buildArguments(proxyConfig));
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig) {
        List<String> arguments = new ArrayList<>();
        
        if(!proxyConfig.isEmpty()){
            Proxy proxy = proxyConfig.getSecureProxy();

            if(proxy == null){
                proxy = proxyConfig.getInsecureProxy();
            }

            arguments.add("--https-proxy=" + proxy.getUri().toString());
            arguments.add("--proxy=" + proxy.getUri().toString());
        }
        
        return arguments;
    }
}
