package com.github.eirslett.maven.plugins.frontend.lib;

public class ArchiveExtractionException extends Exception {

    ArchiveExtractionException(String message) {
        super(message);
    }

    ArchiveExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
