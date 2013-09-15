package com.github.eirslett.maven.plugins.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

final class Utils {
    public static String executeAndGetResult(File workingDirectory, String... command) {
        try {
            Process process = buildPlatformIndependentProcess(command).directory(workingDirectory).start();
            String result = readString(process.getInputStream(), null);
            String error = readString(process.getErrorStream(), null);
            int exitValue = process.exitValue();

            if (exitValue == 0) {
                return result;
            } else if(exitValue == 1) {
                throw new CommandNotFoundException();
            } else {

                throw new CommandExecutionException(error);
            }
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        }
    }

    private static class InputStreamHandler extends Thread {
        private final InputStream inputStream;
        private final Log log;

        private InputStreamHandler(InputStream inputStream, Log log) {
            this.inputStream = inputStream;
            this.log = log;
        }

        public void run(){
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String result = "";
            String line;
            try {
                while((line = reader.readLine()) != null) {
                    result += line + "\n";
                    log.info(result);
                }
            } catch (IOException e) {

            }
        }
    }

    public static int executeAndRedirectOutput(Log logger, File workingDirectory, String... command) {
        try {
            ProcessBuilder builder = buildPlatformIndependentProcess(command).directory(workingDirectory);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            new InputStreamHandler(process.getErrorStream(), logger).start();
            new InputStreamHandler(process.getInputStream(), logger).start();

            return process.waitFor();
        } catch (IOException e) {
            throw new CommandExecutionException(e);
        } catch (InterruptedException e) {
            throw new CommandExecutionException(e);
        }
    }

    public static class CommandExecutionException extends RuntimeException {
        public CommandExecutionException(String message) {
            super(message);
        }

        public CommandExecutionException(Throwable cause) {
            super(cause);
        }
    }

    public static class CommandNotFoundException extends CommandExecutionException {
        public CommandNotFoundException() {
            super("Command was not found (exit value 1)");
        }
    }

    private static ProcessBuilder buildPlatformIndependentProcess(String... command){
        if(isWindows()) {
            return prependWindowsSpecificCommand(command);
        } else {
            return new ProcessBuilder(command);
        }
    }

    private static String readString(InputStream processInputStream, Log logger) throws IOException {
        BufferedReader inputStream = new BufferedReader(new InputStreamReader(processInputStream));
        String result = "";
        String line;
        while((line = inputStream.readLine()) != null) {
            result += line + "\n";
            if (logger != null) {
                logger.info(line);
            }
        }
        return result.trim();
    }

    private static ProcessBuilder prependWindowsSpecificCommand(String... command) {
        List<String> commandList = new ArrayList<String>();
        commandList.addAll(Arrays.asList("cmd", "/C"));
        commandList.addAll(Arrays.asList(command));
        System.out.println("Windows specific command: " + commandList.toString());
        return new ProcessBuilder(commandList);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static interface Loggable {
        public void println(String line);
    }

    public static String joinPath(String... paths) {
        return StringUtils.join(paths, getSeparator());
    }

    private static String getSeparator() {
        return isWindows() ? "\\" : "/";
    }
}
