package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface BowerRunner {

    public void execute(String args) throws TaskRunnerException;
}

final class DefaultBowerRunner extends NodeTaskExecutor implements BowerRunner {

    private static final String TASK_NAME = "bower";
    private static final String TASK_LOCATION = "/node_modules/bower/bin/bower";

    DefaultBowerRunner(Platform platform, File workingDirectory, ProxyConfig proxy) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, buildArguments(proxy));
    }

    private static List<String> buildArguments(ProxyConfig proxy) {
        List<String> arguments = new ArrayList<String>();
        if (proxy != null) {
            if (proxy.isSecure()) {
                arguments.add("--config.https-proxy=" + proxy.getUri().toString());
            } else {
                arguments.add("--config.proxy=" + proxy.getUri().toString());
                arguments.add("--config.registry=http://bower.herokuapp.com");
            }
        }
        return arguments;
    }
}
