package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Mojo(name = "bower", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class BowerMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * Bower arguments. Default is empty (runs just the "bower" command).
     */
    @Parameter(property = "arguments")
    private String arguments; 

    @Parameter(property = "environmentVariables")
    private String environmentVariables;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            MojoUtils.setSLF4jLogger(getLog());
            new FrontendPluginFactory(workingDirectory).getBowerRunner(processEnvs(environmentVariables)).execute(arguments);
        } catch (TaskRunnerException e) {
            throw new MojoFailureException("Failed to run task", e);
        }
    }

    private static Map<String, String> processEnvs(String envs) {
        HashMap<String, String> mapEnvs = new HashMap<String, String>();
        if (envs == null || envs.length() == 0) {
            return mapEnvs;
        }
        String[] split = envs.split(" ");
        for (int i = 0; i < split.length - 1; i++) {
            mapEnvs.put(split[i], split[i + 1]);
        }
        return mapEnvs;
    }
}
