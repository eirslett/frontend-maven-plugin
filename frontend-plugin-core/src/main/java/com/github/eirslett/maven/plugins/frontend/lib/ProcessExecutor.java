package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

final class ProcessExecutionException extends Exception {
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
            final String error = readString(process.getInputStream());
            final int exitValue = process.waitFor();

            if(exitValue == 0){
                return result;
            } else {
                throw new RuntimeException(error);
            }
        } catch (IOException e) {
            throw new ProcessExecutionException(e);
        } catch (InterruptedException e) {
            throw new ProcessExecutionException(e);
        }
    }

    public int executeAndRedirectOutput(final Logger logger) throws ProcessExecutionException {
        try {
            processBuilder.redirectErrorStream(true);
            final Process process = processBuilder.start();

            InputStreamHandler.logInfo(process.getInputStream(), logger).start();
            InputStreamHandler.logError(process.getErrorStream(), logger).start();

            return process.waitFor();
        } catch (IOException e) {
            throw new ProcessExecutionException(e);
        } catch (InterruptedException e) {
            throw new ProcessExecutionException(e);
        }
    }

    private ProcessBuilder createProcessBuilder(){
        return new ProcessBuilder(getPlatformIndependentCommand()).directory(workingDirectory);
    }

    private List<String> getPlatformIndependentCommand(){
        if(platform.isWindows()){
            return Utils.merge(Arrays.asList("cmd", "/C"), command);
        } else {
            return command;
        }
    }

    private static String readString(InputStream processInputStream) throws IOException {
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(processInputStream));
        String result = "";
        String line;
        while((line = inputStream.readLine()) != null) {
            result += line + "\n";
        }
        return result.trim();
    }
}
