package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.NpmRunner;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static com.github.eirslett.maven.plugins.frontend.mojo.MojoUtils.setSLF4jLogger;

@Mojo(name="npm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmMojo extends AbstractNodeJSMojo {

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "arguments", required = false)
    private String arguments;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            setSLF4jLogger(getLog());

            ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session);
            
            NpmRunner npmRunner = new FrontendPluginFactory(workingDirectory, proxyConfig, useGlobal).getNpmRunner();
			npmRunner.execute(arguments);
        } catch (TaskRunnerException e) {
            throw new MojoFailureException("Failed to run task", e);
        }
    }
}
