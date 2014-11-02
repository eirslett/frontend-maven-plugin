package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import static com.github.eirslett.maven.plugins.frontend.mojo.MojoUtils.setSLF4jLogger;

@Mojo(name="npm",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpmMojo extends AbstractMojo {

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "arguments", required = false)
    private String arguments;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;
    
    @Component
    private BuildContext buildContext;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File packageJson = new File(workingDirectory, "package.json");
        if (buildContext == null || buildContext.hasDelta(packageJson) || !buildContext.isIncremental()) {
            try {
                setSLF4jLogger(getLog());

                ProxyConfig proxyConfig = MojoUtils.getProxyConfig(session);
                new FrontendPluginFactory(workingDirectory, proxyConfig).getNpmRunner().execute(arguments);
            } catch (TaskRunnerException e) {
                throw new MojoFailureException("Failed to run task", e);
            }
        } else {
            getLog().info("Skipping npm install as package.json unchanged");
        }
    }
}
