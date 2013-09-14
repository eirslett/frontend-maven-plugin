package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

public class NodeExecutorTest {
    @Test
    @Ignore
    public void testExecute() throws Exception {
        new NodeExecutor(new File("D:\\workspace\\frontend-maven-plugin\\example"), new DefaultLog(new ConsoleLogger(0, "FOO"))).execute("node\\npm\\bin\\npm-cli.js install");
    }
}
