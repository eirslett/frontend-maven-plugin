package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.Map;

interface NodeTaskRunner {
	void execute(String args, String nodeArguments, Map<String,String> environment) throws TaskRunnerException;
}
