package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

class MojoUtils {
    static <E extends Throwable> MojoFailureException toMojoFailureException(E e){
        return new MojoFailureException(e.getMessage()+": "+e.getCause().getMessage());
    }

    static void setSLF4jLogger(Log log){
        StaticLoggerBinder.getSingleton().setLog(log);
    }

    static Proxy getProxy(MavenSession mavenSession){
        return mavenSession.getSettings().getActiveProxy();
    }
}
