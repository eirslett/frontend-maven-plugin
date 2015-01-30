package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface NpmRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";
    static final String TASK_LOCATION = "/node/npm/bin/npm-cli.js";

    public DefaultNpmRunner(Platform platform, File nodeInstallDirectory, File workingDirectory, ProxyConfig proxy) {
        super(TASK_NAME, TASK_LOCATION, nodeInstallDirectory, workingDirectory, platform, buildArguments(proxy));
    }

    private static List<String> buildArguments(ProxyConfig proxy) {
        List<String> arguments = new ArrayList<String>();
        arguments.add("--color=false");
        if (proxy != null) {
            if(proxy.isSecure()){
                arguments.add("--https-proxy=" + proxy.getUri().toString());
            } else {
                arguments.add("--proxy=" + proxy.getUri().toString());
            }
        }
        return arguments;
    }
}
