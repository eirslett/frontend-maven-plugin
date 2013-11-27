package com.github.eirslett.maven.plugins.frontend;

import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.Utils.implode;
import static com.github.eirslett.maven.plugins.frontend.Utils.normalize;
import static com.github.eirslett.maven.plugins.frontend.Utils.prepend;

public abstract class NodeTaskExecutor {
    private final Logger logger;
    private final String taskName;
    private final String taskLocation;
    private final Platform platform;
    private final File workingDirectory;

    public NodeTaskExecutor(String taskName, String taskLocation, File workingDirectory, Platform platform, Logger logger) {
        this.logger = logger;
        this.taskName = taskName;
        this.taskLocation = taskLocation;
        this.platform = platform;
        this.workingDirectory = workingDirectory;
    }

    public final void execute(String args) throws TaskRunnerException {
        final String absoluteTaskLocation = workingDirectory + normalize(taskLocation);
        final List<String> arguments = getArguments(args);
        logger.info("Running " + taskToString(taskName, arguments) + " in " + workingDirectory);

        try {
            final int result = new NodeExecutor(workingDirectory, prepend(absoluteTaskLocation, arguments), platform).executeAndRedirectOutput(logger);
            if(result != 0){
                throw new TaskRunnerException(taskToString(taskName, arguments) + " failed. (error code "+result+")");
            }
        } catch (ProcessExecutionException e) {
            throw new TaskRunnerException(taskToString(taskName, arguments) + " failed.");
        }
    }

    private List<String> getArguments(String args) {
        List<String> arguments =  new ArrayList<String>();
        if(args != null && !args.equals("null") && !args.isEmpty()) {
            arguments.addAll(Arrays.asList(args.split("\\s+")));
        }

        final String noColor = "--no-color";
        if(!arguments.contains(noColor)){
            arguments.add(noColor);
        }
        return arguments;
    }

    private static String taskToString(String taskName, List<String> commands) {
        return "'" + taskName + " " + implode(" ",commands) + "'";
    }
}
