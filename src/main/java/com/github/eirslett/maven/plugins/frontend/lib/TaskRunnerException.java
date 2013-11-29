package com.github.eirslett.maven.plugins.frontend.lib;

public class TaskRunnerException extends Exception {
    TaskRunnerException(String message) {
        super(message);
    }

    TaskRunnerException(String message, Throwable cause){
        super(message, cause);
    }
}
