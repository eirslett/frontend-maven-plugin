package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;

public interface PnpmRunner extends NodeTaskRunner {}

final class DefaultPnpmRunner extends NodeTaskExecutor implements PnpmRunner {
    static final String TASK_NAME = "pnpm";

    public DefaultPnpmRunner(NodeExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, config.getPnpmPath().getAbsolutePath(), buildArguments(proxyConfig, npmRegistryURL));

        if (!config.getPnpmPath().exists() && config.getPnpmCjsPath().exists()) {
            setTaskLocation(config.getPnpmCjsPath().getAbsolutePath());
        }
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
                arguments.add("--noproxy=" + nonProxyHosts.replace('|',','));
            }
        }
        
        return arguments;
    }
}
