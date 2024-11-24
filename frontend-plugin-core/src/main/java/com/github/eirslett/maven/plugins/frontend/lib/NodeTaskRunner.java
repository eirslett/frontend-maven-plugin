package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;

import java.util.Map;

interface NodeTaskRunner {
	void execute(String args, Map<String,String> environment) throws TaskRunnerException;

	Runtime getRuntime() throws TaskRunnerException;
}
