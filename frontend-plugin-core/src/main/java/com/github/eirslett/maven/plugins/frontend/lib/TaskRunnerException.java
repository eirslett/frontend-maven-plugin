package com.github.eirslett.maven.plugins.frontend.lib;

public class TaskRunnerException extends FrontendException {

    private int exitCode;

    TaskRunnerException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    TaskRunnerException(String message, Throwable cause, int exitCode){
        super(message, cause);
        this.exitCode = exitCode;
    }

    TaskRunnerException(String message) {
        this(message, -1);
    }

    TaskRunnerException(String message, Throwable cause){
        this(message, cause, -1);
    }

    public int getExitCode() {
        return exitCode;
    }

}
