package com.github.eirslett.maven.plugins.frontend;

import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.Utils.normalize;

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

    public void execute(String arguments) throws TaskRunnerException {
        try {
            final String absoluteTaskLocation = workingDirectory + normalize(taskLocation);

            logger.info("Running " + taskToString(taskName, arguments) + " in " + workingDirectory);

            List<String> commands =  new ArrayList(Arrays.asList(absoluteTaskLocation));
            if(arguments != null && !arguments.equals("null") && !arguments.isEmpty()) {
                commands.addAll(Arrays.asList(arguments.split("\\s+")));
            }

            final String noColor = "--no-color";
            if(!commands.contains(noColor)){
                commands.add(noColor);
            }

            final int result = new NodeExecutor(workingDirectory, commands, platform).executeAndRedirectOutput(logger);
            if(result != 0){
                throw new TaskRunnerException("'"+taskName+" "+arguments+"' failed.");
            }
        } catch (ProcessExecutionException e) {
            throw new TaskRunnerException(taskToString(taskName, arguments) + " failed.");
        }
    }

    private static String taskToString(String taskName, String arguments) {
        return "'" + taskName + " " + arguments + "'";
    }
}
