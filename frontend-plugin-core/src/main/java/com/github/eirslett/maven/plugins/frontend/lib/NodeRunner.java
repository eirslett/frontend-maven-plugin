package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class NodeRunner  {
    public static final String TASK_NAME = "node";
    public static final String TASK_LOCATION = "node/node";
    private final NodeExecutorConfig config;
    private final ArgumentsParser argumentsParser;
    private final Logger logger = LoggerFactory.getLogger(NodeRunner.class);

    public NodeRunner(NodeExecutorConfig config) {
        super();
        this.config = config;
        argumentsParser = new ArgumentsParser();
    }

    public void execute(String arguments, Map<String, String> environmentVariables) throws FrontendException {
        NodeExecutor nodeExecutor = new NodeExecutor(config, getArguments(arguments), environmentVariables);
        try {
            nodeExecutor.executeAndRedirectOutput(logger);
        } catch (ProcessExecutionException e) {
            throw new FrontendException("executing node "+arguments, e);
        }
    }

    private List<String> getArguments(String arguments) {
        return argumentsParser.parse(arguments);
    }
}