package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

final class ProcessExecutionException extends Exception {
    public ProcessExecutionException(String message) {
        super(message);
    }
    public ProcessExecutionException(Throwable cause) {
        super(cause);
    }
}

final class ProcessExecutor {
    private final File workingDirectory;
    private final List<String> command;
    private final ProcessBuilder processBuilder;
    private final Platform platform;

    public ProcessExecutor(File workingDirectory, List<String> command, Platform platform){
        this.workingDirectory = workingDirectory;
        this.command = command;
        this.platform = platform;

        this.processBuilder = createProcessBuilder();
    }

    public String executeAndGetResult() throws ProcessExecutionException {
        try {
            final Process process = processBuilder.start();
            final String result = readString(process.getInputStream());
            final String error = readString(process.getErrorStream());
            final int exitValue = process.waitFor();

            if(exitValue == 0){
                return result;
            } else {
                throw new ProcessExecutionException(result+" "+error);
            }
        } catch (IOException e) {
            throw new ProcessExecutionException(e);
        } catch (InterruptedException e) {
            throw new ProcessExecutionException(e);
        }
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        try {
            final Process process = processBuilder.start();

            final Thread infoLogThread = InputStreamHandler.logInfo(process.getInputStream(), logger);
            infoLogThread.start();
            final Thread errorLogThread = InputStreamHandler.logError(process.getErrorStream(), logger);
            errorLogThread.start();

            int result = process.waitFor();
            infoLogThread.join();
            errorLogThread.join();
            return result;
        } catch (IOException e) {
            throw new ProcessExecutionException(e);
        } catch (InterruptedException e) {
            throw new ProcessExecutionException(e);
        }
    }

    private ProcessBuilder createProcessBuilder(){
        ProcessBuilder pbuilder = new ProcessBuilder(command).directory(workingDirectory);
        final Map<String, String> environment = pbuilder.environment();
        String pathVarName = "PATH";
        String pathVarValue = environment.get(pathVarName);
        if (platform.isWindows()) {
            for (String key:environment.keySet()) {
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
        pathBuilder.append(workingDirectory + File.separator + "node").append(File.pathSeparator);
        pathBuilder.append(workingDirectory + File.separator + "node" + File.separator + "npm" + File.separator + "bin");
        environment.put(pathVarName, pathBuilder.toString());

        return pbuilder;
    }

    private static String readString(InputStream processInputStream) throws IOException {
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(processInputStream));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = inputStream.readLine()) != null) {
            result.append(line).append("\n");
        }
        return result.toString().trim();
    }
}
