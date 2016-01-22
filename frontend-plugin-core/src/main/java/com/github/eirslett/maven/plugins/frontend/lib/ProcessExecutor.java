package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.slf4j.Logger;

final class ProcessExecutionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ProcessExecutionException(String message) {
        super(message);
    }
    public ProcessExecutionException(Throwable cause) {
        super(cause);
    }
}

final class ProcessExecutor {
    private Map<String, String> environment;
    private CommandLine commandLine;
    private final Executor executor;

    public ProcessExecutor(File workingDirectory, List<String> paths, String executable, List<String> arguments,
            Platform platform) {
        this(workingDirectory, paths, executable, arguments, platform, 0);
    }

    public ProcessExecutor(File workingDirectory, List<String> paths, String executable, List<String> arguments,
            Platform platform, long timeoutInSeconds) {
        this.environment = createEnvironment(paths, platform);
        this.commandLine = createCommandLine(executable, arguments);
        this.executor = createExecutor(workingDirectory, timeoutInSeconds);
    }

    public String executeAndGetResult(final Logger logger) throws ProcessExecutionException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitValue = execute(logger, stdout, stderr);
        if (exitValue == 0) {
            return stdout.toString().trim();
        } else {
            throw new ProcessExecutionException(stdout + " " + stderr);
        }
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        OutputStream stdout = new LoggerOutputStream(logger, 0);
        OutputStream stderr = new LoggerOutputStream(logger, 1);

        return execute(logger, stdout, stderr);
    }

    private int execute(final Logger logger, final OutputStream stdout, final OutputStream stderr)
            throws ProcessExecutionException {
        logger.debug("Executing command line {}", commandLine);
        try {
            ExecuteStreamHandler streamHandler = new PumpStreamHandler(stdout, stderr);
            executor.setStreamHandler(streamHandler);

            int exitValue = executor.execute(commandLine, environment);
            logger.debug("Exit value {}", exitValue);

            return exitValue;
        } catch (ExecuteException e) {
            if (executor.getWatchdog() != null && executor.getWatchdog().killedProcess()) {
                throw new ProcessExecutionException("Process killed after timeout");
            }
            throw new ProcessExecutionException(e);
        } catch (IOException e) {
            throw new ProcessExecutionException(e);
        }
    }

    private Map<String, String> createEnvironment(List<String> paths, Platform platform) {
        final Map<String, String> environment = new HashMap<String, String>(System.getenv());
        String pathVarName = "PATH";
        String pathVarValue = environment.get(pathVarName);
        if (platform.isWindows()) {
            for (String key : environment.keySet()) {
                if ("PATH".equalsIgnoreCase(key)) {
                    pathVarName = key;
                    pathVarValue = environment.get(key);
                }
            }
        }

        StringBuilder pathBuilder = new StringBuilder();
        if (pathVarValue != null) {
            pathBuilder.append(pathVarValue).append(File.pathSeparator);
        }
        for (String path : paths) {
            pathBuilder.insert(0, File.pathSeparator).insert(0, path);
        }
        environment.put(pathVarName, pathBuilder.toString());

        return environment;
    }

    private CommandLine createCommandLine(String executable, List<String> arguments) {
        CommandLine commmandLine = new CommandLine(executable);

        for (String argument : arguments) {
            commmandLine.addArgument(argument);
        }

        return commmandLine;
    }

    private Executor createExecutor(File workingDirectory, long timeoutInSeconds) {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setWorkingDirectory(workingDirectory);
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());   // Fixes #41

        if (timeoutInSeconds > 0) {
            executor.setWatchdog(new ExecuteWatchdog(timeoutInSeconds * 1000));
        }

        return executor;
    }

    private class LoggerOutputStream extends LogOutputStream {
        private final Logger logger;

        public LoggerOutputStream(Logger logger, int logLevel) {
            super(logLevel);
            this.logger = logger;
        }

        @Override
        public final void flush() {
            // buffer processing on close() only
        }

        @Override
        protected void processLine(final String line, final int logLevel) {
            if (logLevel == 0) {
                logger.info(line);
            } else {
                // FIXME: workaround for #343 -> delegate this check (via callback) to specific NodeTaskExecutor runner implementation 
                if (line.startsWith("npm WARN ")) {
                    logger.warn(line);
                } else {
                    logger.error(line);
                }
            }
        }
    }
}
