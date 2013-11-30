package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

class MojoUtils {
    static <E extends Throwable> MojoFailureException toMojoFailureException(E e){
        return new MojoFailureException(e.getMessage()+": "+e.getCause().getMessage());
    }

    static Logger getSlf4jLogger(Log log, Class clazz){
        StaticLoggerBinder.getSingleton().setLog(log);
        return LoggerFactory.getLogger(clazz);
    }
}
