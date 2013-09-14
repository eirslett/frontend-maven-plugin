package com.github.eirslett.maven.plugins.frontend;

import org.apache.maven.plugin.logging.Log;

import java.io.File;

import static com.github.eirslett.maven.plugins.frontend.Utils.executeAndGetResult;
import static com.github.eirslett.maven.plugins.frontend.Utils.executeAndRedirectOutput;

public class NodeExecutor {
    private final File baseDir;
    private final Log log;
    private final OS os;
    private final String node;

    public NodeExecutor(File baseDir, Log log){
        this(baseDir, log, OS.guess());
    }

    public NodeExecutor(File baseDir, Log log, OS os){
        this.baseDir = baseDir;
        this.log = log;
        this.os = os;
        this.node = baseDir + File.separator + "node" + File.separator + "node";
    }

    public int execute(final String command){
        return executeAndRedirectOutput(log, baseDir, node + " " + command);
    }

    public String executeWithResult(final String command){
        return executeAndGetResult(baseDir, node + " " + command);
    }
}
