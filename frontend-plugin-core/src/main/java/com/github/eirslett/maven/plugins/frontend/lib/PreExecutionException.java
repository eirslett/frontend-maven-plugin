package com.github.eirslett.maven.plugins.frontend.lib;

public class PreExecutionException extends FrontendException {
  public PreExecutionException(String message) {
    super(message);
  }

  public PreExecutionException(String message, Throwable cause){
    super(message, cause);
  }
}
