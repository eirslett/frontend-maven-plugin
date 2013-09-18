package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Ignore;
import org.junit.Test;

public class InstallNodeAndNpmMojoTest {
    @Test
    @Ignore
    public void testExecute() throws Exception {
//        new InstallNodeAndNpmMojo("v0.10.18", "1.3.8", "D:\\workspace\\frontend-maven-plugin\\example", new DefaultLog(new ConsoleLogger(0, "FOO")), OS.Windows, Architecture.x64).execute();
        new NodeAndNPMInstaller("v0.9.6", "1.1.69", "D:\\workspace\\frontend-maven-plugin\\example", new DefaultLog(new ConsoleLogger(0, "FOO")), OS.Mac, Architecture.x64).install();
    }
}
