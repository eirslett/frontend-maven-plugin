package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.lib.Utils.*;

abstract class NodeTaskExecutor {
    private static final String DS = "//";
    private static final String AT = "@";
    
    private final Logger logger;
    private final String taskName;
    private final String taskLocation;
    private final Platform platform;
    private final File workingDirectory;
    private final List<String> additionalArguments;

    public NodeTaskExecutor(String taskName, String taskLocation, File workingDirectory, Platform platform, List<String> additionalArguments) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.taskName = taskName;
        this.taskLocation = taskLocation;
        this.platform = platform;
        this.workingDirectory = workingDirectory;
        this.additionalArguments = additionalArguments;
    }

    public final void execute(String args) throws TaskRunnerException {
        final String absoluteTaskLocation = workingDirectory + normalize(taskLocation);
        final List<String> arguments = getArguments(args);
        logger.info("Running " + taskToString(taskName, arguments) + " in " + workingDirectory);

        try {
            final int result = new NodeExecutor(workingDirectory, prepend(absoluteTaskLocation, arguments), platform).executeAndRedirectOutput(logger);
            if (result != 0) {
                throw new TaskRunnerException(taskToString(taskName, arguments) + " failed. (error code " + result + ")");
            }
        } catch (ProcessExecutionException e) {
            throw new TaskRunnerException(taskToString(taskName, arguments) + " failed.", e);
        }
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

    private static String taskToString(String taskName, List<String> arguments) {
        List<String> clonedArguments = new ArrayList<String>(arguments);
        for (int i = 0; i < clonedArguments.size(); i++) {
            final String s = clonedArguments.get(i);
            final boolean maskMavenProxyPassword = s.contains("proxy=");
            if (maskMavenProxyPassword) {
                final String bestEffortMaskedPassword = maskPassword(s);
                clonedArguments.set(i, bestEffortMaskedPassword);
            }
        }
        return "'" + taskName + " " + implode(" ", clonedArguments) + "'";
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
