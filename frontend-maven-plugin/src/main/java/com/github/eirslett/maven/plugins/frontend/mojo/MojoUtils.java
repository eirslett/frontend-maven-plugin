package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.slf4j.impl.StaticLoggerBinder;

class MojoUtils {
    static <E extends Throwable> MojoFailureException toMojoFailureException(E e){
        return new MojoFailureException(e.getMessage()+": "+e.getCause().getMessage(), e);
    }

    static void setSLF4jLogger(Log log){
        StaticLoggerBinder.getSingleton().setLog(log);
    }

    static ProxyConfig getProxyConfig(MavenSession mavenSession){
        if(
                mavenSession == null ||
                mavenSession.getSettings() == null ||
                mavenSession.getSettings().getActiveProxy() == null ||
                !mavenSession.getSettings().getActiveProxy().isActive()
                ){
            return null;
        } else {
            Proxy mavenProxy = mavenSession.getSettings().getActiveProxy();
            return new ProxyConfig(
                    mavenProxy.getProtocol(),
                    mavenProxy.getHost(),
                    mavenProxy.getPort(),
                    mavenProxy.getUsername(),
                    mavenProxy.getPassword());
        }
    }
}
