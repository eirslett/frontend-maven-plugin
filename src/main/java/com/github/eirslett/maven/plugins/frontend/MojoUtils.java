package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

public class MojoUtils {
    public static <E extends Throwable> MojoFailureException toMojoFailureException(E e){
        return new MojoFailureException(e.getMessage()+": "+e.getCause().getMessage());
    }

    public static Logger getSlf4jLogger(Log log, Class clazz){
        StaticLoggerBinder.getSingleton().setLog(log);
        return LoggerFactory.getLogger(clazz);
    }
}
