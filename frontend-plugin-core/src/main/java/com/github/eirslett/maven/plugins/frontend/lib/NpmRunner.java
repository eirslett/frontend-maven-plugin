package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface NpmRunner extends NodeTaskRunner {}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    private static final String TASK_NAME = "npm";
    private final NpmRegistryAuthHandler npmRegistryAuthHandler;

    public DefaultNpmRunner(NodeExecutorConfig config, ProxyConfig proxyConfig, NpmRegistryConfig registryConfig) {
        super(config, TASK_NAME, config.getNpmPath().getAbsolutePath(),
                buildArguments(proxyConfig, registryConfig != null ? registryConfig.getUrl() : null));
        this.npmRegistryAuthHandler = new DefaultNpmRegistryAuthHandler(config, proxyConfig, registryConfig);
    }

    @Override
    protected void beforeExecute(List<String> arguments, Map<String, String> environment) throws Exception {
        npmRegistryAuthHandler.handle(arguments, environment);
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig, String npmRegistryURL) {
        List<String> arguments = new ArrayList<String>();
               
        if(npmRegistryURL != null && !npmRegistryURL.isEmpty()){
            arguments.add ("--registry=" + npmRegistryURL);
        }

        if(!proxyConfig.isEmpty()){
            Proxy proxy = null;
            if(npmRegistryURL != null && !npmRegistryURL.isEmpty()){
                proxy = proxyConfig.getProxyForUrl(npmRegistryURL);
            }

            if(proxy == null){
                proxy = proxyConfig.getSecureProxy();
            }

            if(proxy == null){
                proxy = proxyConfig.getInsecureProxy();
            }

            arguments.add("--https-proxy=" + proxy.getUri().toString());
            arguments.add("--proxy=" + proxy.getUri().toString());
        }
        
        return arguments;
    }
}
