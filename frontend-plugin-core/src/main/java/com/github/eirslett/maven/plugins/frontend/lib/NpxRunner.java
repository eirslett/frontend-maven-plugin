package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;

public interface NpxRunner extends NodeTaskRunner {}

final class DefaultNpxRunner extends NodeTaskExecutor implements NpxRunner {
    static final String TASK_NAME = "npx";

    public DefaultNpxRunner(NodeExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, config.getNpxPath().getAbsolutePath(), buildNpmArguments(proxyConfig, npmRegistryURL));
    }

    // Visible for testing only.
    /**
     * These are, in fact, npm arguments, that need to be split from the npx arguments by '--'.
     *
     * See an example:
     * npx some-package -- --registry=http://myspecialregisty.com
     */
    static List<String> buildNpmArguments(ProxyConfig proxyConfig, String npmRegistryURL) {
        List<String> arguments = new ArrayList<>();
               
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

        List<String> npmArguments;
        if (arguments.isEmpty()) {
            npmArguments = arguments;
        } else {
            npmArguments = new ArrayList<>();
            npmArguments.add("--");
            npmArguments.addAll(arguments);
        }
        
        return npmArguments;
    }
}
