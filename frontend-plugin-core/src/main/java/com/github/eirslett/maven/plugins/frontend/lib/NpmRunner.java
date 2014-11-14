package com.github.eirslett.maven.plugins.frontend.lib;

import static com.github.eirslett.maven.plugins.frontend.lib.Utils.implode;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.normalize;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.prepend;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NpmRunner {
    public void execute(String args) throws TaskRunnerException;
}

final class DefaultNpmRunner extends NodeTaskExecutor implements NpmRunner {
    static final String TASK_NAME = "npm";
    static final String TASK_LOCATION = "/node/npm/bin/npm-cli.js";

    public DefaultNpmRunner(Platform platform, File workingDirectory, ProxyConfig proxy) {
        super(TASK_NAME, TASK_LOCATION, workingDirectory, platform, buildArguments(proxy), false);
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

final class LocalNpmRunner implements NpmRunner {
    private final String taskName;
	private Platform platform;
	private File workingDirectory;
	private Logger logger;

    public LocalNpmRunner(Platform platform, File workingDirectory) {
    	this.logger = LoggerFactory.getLogger(getClass());
		this.platform = platform;
		this.workingDirectory = workingDirectory;
		this.taskName = platform.isWindows() ? "npm.cmd" : "npm";
    }

    public final void execute(String args) throws TaskRunnerException {
        final List<String> arguments = getArguments(args);
        logger.info("Running " + taskToString(taskName, arguments) + " in " + workingDirectory);

        try {
        	int result = new ProcessExecutor(workingDirectory, prepend(taskName, arguments), platform).executeAndRedirectOutput(logger);
            if(result != 0){
                throw new TaskRunnerException(taskToString(taskName, arguments) + " failed. (error code "+result+")");
            }
        } catch (ProcessExecutionException e) {
            throw new TaskRunnerException(taskToString(taskName, arguments) + " failed.", e);
        }
    }
    
    private List<String> getArguments(String args) {
        List<String> arguments =  new ArrayList<String>();
        if(args != null && !args.equals("null") && !args.isEmpty()) {
            arguments.addAll(Arrays.asList(args.split("\\s+")));
        }

        if(!arguments.contains("--color=false")){
            arguments.add("--color=false");
        }
        return arguments;
    }
    
    private static String taskToString(String taskName, List<String> commands) {
        return "'" + taskName + " " + implode(" ",commands) + "'";
    }
}

