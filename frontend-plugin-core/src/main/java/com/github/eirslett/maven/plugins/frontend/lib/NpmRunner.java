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

    // Visible for testing only.
    static List<String> buildArguments(ProxyConfig proxyConfig, String npmRegistryURL) {
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

            final String nonProxyHosts = proxy.getNonProxyHosts();
            if (nonProxyHosts != null && !nonProxyHosts.isEmpty()) {
                final String[] nonProxyHostList = nonProxyHosts.split("\\|");
                for (String nonProxyHost: nonProxyHostList) {
                    arguments.add("--noproxy=" + nonProxyHost.replace("*", ""));
                }
            }
        }
        
        return arguments;
    }
}
