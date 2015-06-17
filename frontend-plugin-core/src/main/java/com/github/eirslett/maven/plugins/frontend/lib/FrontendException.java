package com.github.eirslett.maven.plugins.frontend.lib;

public class FrontendException extends Exception {

  FrontendException(String message) {
    super(message);
  }

  FrontendException(String message, Throwable cause){
    super(message, cause);
  }
}
