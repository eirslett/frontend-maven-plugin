package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MojoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MojoUtils.class);

    static <E extends Throwable> MojoFailureException toMojoFailureException(E e) {
        String causeMessage = e.getCause() != null ? ": " + e.getCause().getMessage() : "";
        return new MojoFailureException(e.getMessage() + causeMessage, e);
    }

    static ProxyConfig getProxyConfig(MavenSession mavenSession, SettingsDecrypter decrypter) {
        if (mavenSession == null ||
                mavenSession.getSettings() == null ||
                mavenSession.getSettings().getProxies() == null ||
                mavenSession.getSettings().getProxies().isEmpty()) {
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        } else {
            final List<Proxy> mavenProxies = mavenSession.getSettings().getProxies();

            final List<ProxyConfig.Proxy> proxies = new ArrayList<ProxyConfig.Proxy>(mavenProxies.size());

            for (Proxy mavenProxy : mavenProxies) {
                if (mavenProxy.isActive()) {
                    mavenProxy = decryptProxy(mavenProxy, decrypter);
                    proxies.add(new ProxyConfig.Proxy(mavenProxy.getId(), mavenProxy.getProtocol(), mavenProxy.getHost(),
                            mavenProxy.getPort(), mavenProxy.getUsername(), mavenProxy.getPassword(), mavenProxy.getNonProxyHosts()));
                }
            }

            LOGGER.info("Found proxies: {}", proxies);
            return new ProxyConfig(proxies);
        }
    }

    private static Proxy decryptProxy(Proxy proxy, SettingsDecrypter decrypter) {
      synchronized (proxy) {
        final DefaultSettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(proxy);
        SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
        return decryptedResult.getProxy();
      }
    }

    static Server decryptServer(String serverId, MavenSession mavenSession, SettingsDecrypter decrypter) {
        if (StringUtils.isEmpty(serverId)) {
            return null;
        }
        Server server = mavenSession.getSettings().getServer(serverId);
        if (server != null) {
          synchronized (server) {
            final DefaultSettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(server);
            SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
            return decryptedResult.getServer();
          }
        } else {
            LOGGER.warn("Could not find server '" + serverId + "' in settings.xml");
            return null;
        }
    }

    static boolean shouldExecute(BuildContext buildContext, List<File> triggerfiles, File srcdir) {

    // If there is no buildContext, or this is not an incremental build, always execute.
    if (buildContext == null || !buildContext.isIncremental()) {
      return true;
    }

    if (triggerfiles != null) {
      for (File triggerfile : triggerfiles) {
        if (buildContext.hasDelta(triggerfile)) {
          return true;
        }
      }
    }

    if (srcdir == null) {
      return true;
    }

    // Check for changes in the srcdir
    Scanner scanner = buildContext.newScanner(srcdir);
    scanner.scan();
    String[] includedFiles = scanner.getIncludedFiles();
    return (includedFiles != null && includedFiles.length > 0);
  }
}
