package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.Map;

interface YarnTaskRunner {
	void execute(String args, Map<String, String> environment) throws TaskRunnerException;
}
