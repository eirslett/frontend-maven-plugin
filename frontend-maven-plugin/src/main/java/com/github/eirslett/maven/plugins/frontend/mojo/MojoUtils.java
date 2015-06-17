package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.List;

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
