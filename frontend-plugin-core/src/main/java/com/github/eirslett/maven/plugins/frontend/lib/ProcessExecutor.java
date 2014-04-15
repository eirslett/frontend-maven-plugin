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
            final String error = readString(process.getInputStream());
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
            processBuilder.redirectErrorStream(true);
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
        return new ProcessBuilder(getPlatformIndependentCommand()).directory(workingDirectory);
    }

    private List<String> getPlatformIndependentCommand(){
        if(platform.isWindows()){
            return command;
        } else {
            return Utils.merge(Arrays.asList("sh", workingDirectory+"/node/with_new_path.sh"), command);
        }
    }

    private static String readString(InputStream processInputStream) throws IOException {
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(processInputStream));
        StringBuffer result = new StringBuffer();
        String line;
        while((line = inputStream.readLine()) != null) {
            result.append(line + "\n");
        }
        return result.toString().trim();
    }
}
