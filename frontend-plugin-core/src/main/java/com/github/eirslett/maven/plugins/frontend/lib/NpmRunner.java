package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public interface NpmRunner {
    void execute(String args) throws TaskRunnerException;
}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";

    public DefaultNpmRunner(NodeExecutorConfig config, ProxyConfig proxyConfig) {
        super(config, TASK_NAME, config.getNpmPath().getAbsolutePath(), buildArguments(proxyConfig, config.getWorkingDirectory()));
    }

    private static List<String> buildArguments(ProxyConfig proxyConfig, File workingDirectory) {
        List<String> arguments = new ArrayList<String>();
        arguments.add("--color=false");

        if (!proxyConfig.isEmpty()) {
            // Cannot use `npm get registry` as npm-cli.js may not be installed yet
            String npmRegistry = getNpmRegistryFromConfigFile(workingDirectory);
            if (proxyConfig.getProxyForUrl(npmRegistry) == null) {
                return arguments;
            }

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

    private static String getNpmRegistryFromConfigFile(File workingDirectory) {
        try {
            Properties properties = new Properties();
            properties.load(FileUtils.openInputStream(new File(workingDirectory, ".npmrc")));
            return properties.getProperty("registry", NodeAndNPMInstaller.DEFAULT_NPM_REGISTRY);
        } catch (IOException e) {
            return NodeAndNPMInstaller.DEFAULT_NPM_REGISTRY;
        }
    }
}
