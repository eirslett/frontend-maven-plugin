package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

final class InputStreamHandler extends Thread {
    private interface LogLevelAgnosticLogger {
        public void log(String value);
    }

    private final InputStream inputStream;
    private final LogLevelAgnosticLogger logger;

    private InputStreamHandler(InputStream inputStream, LogLevelAgnosticLogger logger) {
        this.inputStream = inputStream;
        this.logger = logger;
    }

    public static InputStreamHandler logInfo(InputStream inputStream, Logger logger){
        return new InputStreamHandler(inputStream, infoLoggerFor(logger));
    }

    public static InputStreamHandler logError(InputStream inputStream, Logger logger){
        return new InputStreamHandler(inputStream, errorLoggerFor(logger));
    }

    public void run(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while((line = reader.readLine()) != null) {
                logger.log(line);
            }
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }

    private static LogLevelAgnosticLogger infoLoggerFor(final Logger logger){
        return new LogLevelAgnosticLogger() {
            @Override
            public void log(String value) {
                logger.info(value);
            }
        };
    }

    private static LogLevelAgnosticLogger errorLoggerFor(final Logger logger){
        return new LogLevelAgnosticLogger() {
            @Override
            public void log(String value) {
                logger.error(value);
            }
        };
    }
}
