package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

class MojoUtils {
    static <E extends Throwable> MojoFailureException toMojoFailureException(E e) {
        return new MojoFailureException(e.getMessage() + ": " + e.getCause().getMessage(), e);
    }

    static ProxyConfig getProxyConfig(MavenSession mavenSession, SettingsDecrypter decrypter) {
        if (mavenSession == null ||
                mavenSession.getSettings() == null ||
                mavenSession.getSettings().getActiveProxy() == null ||
                !mavenSession.getSettings().getActiveProxy().isActive()) {
            return null;
        } else {
            Proxy mavenProxy = mavenSession.getSettings().getActiveProxy();
            final DefaultSettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(mavenProxy);
            SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
            mavenProxy = decryptedResult.getProxy();
            return new ProxyConfig(mavenProxy.getId(), mavenProxy.getProtocol(), mavenProxy.getHost(),
                    mavenProxy.getPort(), mavenProxy.getUsername(), mavenProxy.getPassword());
        }
    }
}
