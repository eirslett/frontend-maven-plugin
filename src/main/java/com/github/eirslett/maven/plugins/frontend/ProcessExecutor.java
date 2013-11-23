package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static com.github.eirslett.maven.plugins.frontend.Utils.merge;

final class ProcessExecutor {
    private final File workingDirectory;
    private final List<String> command;
    private final ProcessBuilder processBuilder;
    private final Platform platform;

    public ProcessExecutor(File workingDirectory, List<String> command){
        this(workingDirectory, command, Platform.guess());
    }

    public ProcessExecutor(File workingDirectory, List<String> command, Platform platform){
        this.workingDirectory = workingDirectory;
        this.command = command;
        this.platform = platform;

        this.processBuilder = createProcessBuilder();
    }

    public String executeAndGetResult(){
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
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int executeAndRedirectOutput(final Log logger){
        try {
            processBuilder.redirectErrorStream(true);
            final Process process = processBuilder.start();

            InputStreamHandler.logInfo(process.getInputStream(), logger).start();
            InputStreamHandler.logError(process.getErrorStream(), logger).start();

            return process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ProcessBuilder createProcessBuilder(){
        return new ProcessBuilder(getPlatformIndependentCommand()).directory(workingDirectory);
    }

    private List<String> getPlatformIndependentCommand(){
        if(platform.isWindows()){
            return merge(Arrays.asList("cmd", "/C"), command);
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
