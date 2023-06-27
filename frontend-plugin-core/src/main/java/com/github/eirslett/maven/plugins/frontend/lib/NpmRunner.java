package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

public interface NpmRunner extends NodeTaskRunner {}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";

    public DefaultNpmRunner(NodeExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, config.getNpmPath().getAbsolutePath(), buildArguments(proxyConfig, npmRegistryURL),
        		buildProxy(proxyConfig, npmRegistryURL));
    }

    // Visible for testing only.
    static List<String> buildArguments(ProxyConfig proxyConfig, String npmRegistryURL) {
        List<String> arguments = new ArrayList<String>();

        if(npmRegistryURL != null && !npmRegistryURL.isEmpty()){
            arguments.add ("--registry=" + npmRegistryURL);
        }

        if(!proxyConfig.isEmpty()){
            Proxy proxy = getProxyConfig(proxyConfig, npmRegistryURL);

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

    private static Map<String, String> buildProxy(ProxyConfig proxyConfig, String npmRegistryURL) {
        Map<String, String> proxyEnvironmentVariables = Collections.emptyMap();

        if(!proxyConfig.isEmpty()){
            Proxy proxy = getProxyConfig(proxyConfig, npmRegistryURL);
            proxyEnvironmentVariables = new HashMap<>();

            proxyEnvironmentVariables.put("https_proxy", proxy.getUri().toString());
            proxyEnvironmentVariables.put("http_proxy", proxy.getUri().toString());
        }

        return proxyEnvironmentVariables;
    }

	private static Proxy getProxyConfig(ProxyConfig proxyConfig, String npmRegistryURL) {
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
		return proxy;
	}
}
