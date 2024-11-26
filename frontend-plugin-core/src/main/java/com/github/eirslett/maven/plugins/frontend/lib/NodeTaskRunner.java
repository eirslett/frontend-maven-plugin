package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;

import java.util.Map;
import java.util.Optional;

interface NodeTaskRunner {
	void execute(String args, Map<String,String> environment) throws TaskRunnerException;

	Optional<Runtime> getRuntime();
}
