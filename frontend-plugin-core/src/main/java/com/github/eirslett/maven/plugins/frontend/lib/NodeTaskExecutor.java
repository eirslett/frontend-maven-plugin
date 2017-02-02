package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.lib.Utils.implode;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.normalize;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.prepend;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.merge;
import java.util.Map;

abstract class NodeTaskExecutor {
    private static final String DS = "//";
    private static final String AT = "@";
    
    private final Logger logger;
    private final String taskName;
    private final String taskLocation;
    private final List<String> additionalArguments;
    private final NodeExecutorConfig config;
    private final List<String> additionalNodeArguments;

    public NodeTaskExecutor(NodeExecutorConfig config, String taskLocation) {
        this(config, taskLocation, Collections.<String>emptyList());
    }

    public NodeTaskExecutor(NodeExecutorConfig config, String taskName, String taskLocation) {
        this(config, taskName, taskLocation, Collections.<String>emptyList());
    }

    public NodeTaskExecutor(NodeExecutorConfig config, String taskLocation, List<String> additionalArguments) {
        this(config, getTaskNameFromLocation(taskLocation), taskLocation, additionalArguments);
    }

    public NodeTaskExecutor(NodeExecutorConfig config, String taskName, String taskLocation, List<String> additionalArguments) {
        this(config, taskName, taskLocation, additionalArguments, Collections.<String>emptyList());
    }

    public NodeTaskExecutor(NodeExecutorConfig config, String taskName, String taskLocation, List<String> additionalArguments, List<String> additionalNodeArguments) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.config = config;
        this.taskName = taskName;
        this.taskLocation = taskLocation;
        this.additionalArguments = additionalArguments;
        this.additionalNodeArguments = additionalNodeArguments;
    }

    private static String getTaskNameFromLocation(String taskLocation) {
        return taskLocation.replaceAll("^.*/([^/]+)(?:\\.js)?$","$1");
    }

    public final void execute(String args, String nodeArgs, Map<String, String> environment) throws TaskRunnerException {
        final String absoluteTaskLocation = getAbsoluteTaskLocation();
        final List<String> arguments = getArguments(args);
        final List<String> nodeArguments = getNodeArguments(nodeArgs);
        logger.info("Running " + taskToString(taskName, arguments, nodeArguments) + " in " + config.getWorkingDirectory());

        try {
            final int result = new NodeExecutor(config, merge(nodeArguments,prepend(absoluteTaskLocation, arguments)), environment).executeAndRedirectOutput(logger);
            if (result != 0) {
                throw new TaskRunnerException(taskToString(taskName, arguments, nodeArguments) + " failed. (error code " + result + ")");
            }
        } catch (ProcessExecutionException e) {
            throw new TaskRunnerException(taskToString(taskName, arguments, nodeArguments) + " failed.", e);
        }
    }

    private String getAbsoluteTaskLocation() {
        String location = normalize(taskLocation);
        if (Utils.isRelative(taskLocation)) {
            location = new File(config.getWorkingDirectory(), location).getAbsolutePath();
        }
        return location;
    }

    private List<String> getArguments(String args) {
        List<String> arguments = new ArrayList<String>();
        if (args != null && !args.equals("null") && !args.isEmpty()) {
            arguments.addAll(Arrays.asList(args.split("\\s+")));
        }

        for (String argument : additionalArguments) {
            if (!arguments.contains(argument)) {
                arguments.add(argument);
            }
        }
        return arguments;
    }

    private List<String> getNodeArguments(String args) {
        List<String> arguments = new ArrayList<String>();
        if (args != null && !args.equals("null") && !args.isEmpty()) {
            arguments.addAll(Arrays.asList(args.split("\\s+")));
        }

        for (String argument : additionalNodeArguments) {
            if (!arguments.contains(argument)) {
                arguments.add(argument);
            }
        }
        return arguments;
    }

    private static String taskToString(String taskName, List<String> arguments, List<String> nodeArguments) {
        List<String> clonedArguments = new ArrayList<String>(arguments);
        for (int i = 0; i < clonedArguments.size(); i++) {
            final String s = clonedArguments.get(i);
            final boolean maskMavenProxyPassword = s.contains("proxy=");
            if (maskMavenProxyPassword) {
                final String bestEffortMaskedPassword = maskPassword(s);
                clonedArguments.set(i, bestEffortMaskedPassword);
            }
        }
        return "'" + taskName + " " + implode(" ", clonedArguments) + (nodeArguments.isEmpty()?"":(" with node arguments " + implode(" ", nodeArguments))) + "'";
    }

    private static String maskPassword(String proxyString) {
        String retVal = proxyString;
        if (proxyString != null && !"".equals(proxyString.trim())) {
            boolean hasSchemeDefined = proxyString.contains("http:") || proxyString.contains("https:");
            boolean hasProtocolDefined = proxyString.contains(DS);
            boolean hasAtCharacterDefined = proxyString.contains(AT);
            if (hasSchemeDefined && hasProtocolDefined && hasAtCharacterDefined) {
                final int firstDoubleSlashIndex = proxyString.indexOf(DS);
                final int lastAtCharIndex = proxyString.lastIndexOf(AT);
                boolean hasPossibleURIUserInfo = firstDoubleSlashIndex < lastAtCharIndex;
                if (hasPossibleURIUserInfo) {
                    final String userInfo = proxyString.substring(firstDoubleSlashIndex + DS.length(), lastAtCharIndex);
                    final String[] userParts = userInfo.split(":");
                    if (userParts.length > 0) {
                        final int startOfUserNameIndex = firstDoubleSlashIndex + DS.length();
                        final int firstColonInUsernameOrEndOfUserNameIndex = startOfUserNameIndex + userParts[0].length();
                        final String leftPart = proxyString.substring(0, firstColonInUsernameOrEndOfUserNameIndex);
                        final String rightPart = proxyString.substring(lastAtCharIndex);
                        retVal = leftPart + ":***" + rightPart;
                    }
                }
            }
        }
        return retVal;
    }
}
