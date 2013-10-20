package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

final class InputStreamHandler extends Thread {
    private interface Logger{
        public void log(String value);
    }

    private final InputStream inputStream;
    private final Logger logger;

    private InputStreamHandler(InputStream inputStream, Logger logger) {
        this.inputStream = inputStream;
        this.logger = logger;
    }

    public static InputStreamHandler logInfo(InputStream inputStream, Log logger){
        return new InputStreamHandler(inputStream, infoLoggerFor(logger));
    }

    public static InputStreamHandler logError(InputStream inputStream, Log logger){
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

    private static Logger infoLoggerFor(final Log logger){
        return new Logger() {
            @Override
            public void log(String value) {
                logger.info(value);
            }
        };
    }

    private static Logger errorLoggerFor(final Log logger){
        return new Logger() {
            @Override
            public void log(String value) {
                logger.error(value);
            }
        };
    }
}
