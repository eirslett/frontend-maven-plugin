package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.Properties;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import com.github.eirslett.maven.plugins.frontend.lib.PreExecutionException;

public abstract class AbstractFrontendMojo extends AbstractMojo {

    @Component
    protected MojoExecution execution;

    /**
     * Whether you should skip while running in the test phase (default is false)
     */
    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    protected Boolean skipTests;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @since 1.4
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    protected boolean testFailureIgnore;

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    protected File workingDirectory;

    /**
     * The base directory for installing node and npm.
     */
    @Parameter(property = "installDirectory", required = false)
    protected File installDirectory;

    /**
     * File containing environment variables to be passed to the build. Environment variables passed via the
     * {@link AbstractFrontendMojo#environmentVariables} will override the ones present inside the file.
     *
     * @since 1.16
     */
    @Parameter(property = "environmentFile", required = false)
    protected File environmentFile;

    /**
     * Additional environment variables to pass to the build. If used alongside {@link AbstractFrontendMojo#environmentFile} then
     * environment variables here will override the ones present inside the file.
     */
    @Parameter
    protected Map<String, String> environmentVariables;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    /**
     * Determines if this execution should be skipped.
     */
    private boolean skipTestPhase() {
        return skipTests && isTestingPhase();
    }

    /**
     * Determines if the current execution is during a testing phase (e.g., "test" or "integration-test").
     */
    private boolean isTestingPhase() {
        String phase = execution.getLifecyclePhase();
        return "test".equals(phase) || "integration-test".equals(phase);
    }

    protected abstract void execute(FrontendPluginFactory factory) throws FrontendException;

    /**
     * Implemented by children to determine if this execution should be skipped.
     */
    protected abstract boolean skipExecution();

    @Override
    public void execute() throws MojoFailureException {
        if (testFailureIgnore && !isTestingPhase()) {
            getLog().info("testFailureIgnore property is ignored in non test phases");
        }
        if (!(skipTestPhase() || skipExecution())) {
            if (installDirectory == null) {
                installDirectory = workingDirectory;
            }
            try {
                execute(new FrontendPluginFactory(workingDirectory, installDirectory,
                        new RepositoryCacheResolver(repositorySystemSession)));
            } catch (TaskRunnerException e) {
                if (testFailureIgnore && isTestingPhase()) {
                    getLog().error("There are test failures.\nFailed to run task: " + e.getMessage(), e);
                } else {
                    throw new MojoFailureException("Failed to run task", e);
                }
            } catch (FrontendException e) {
                throw MojoUtils.toMojoFailureException(e);
            }
        } else {
            getLog().info("Skipping execution.");
        }
    }

    /**
     *  Computes the environment variables based on the configuration provided. It will first evaluate the
     *  {@link AbstractFrontendMojo#environmentFile} configuration and then the {@link AbstractFrontendMojo#environmentVariables}. In case
     *  the latter one contains environment variables also present in the file, they will be overwritten.
     *  In case the {@link AbstractFrontendMojo#environmentFile} configuration is done but the file cannot be found, an error is printed but
     *  the build will still continue!
     *
     *  @return the aggregated environment variables, may be empty
     *  @throws PreExecutionException when working with the environment file an exception occurs
     */
    protected Map<String, String> getEnvironmentVariables() throws PreExecutionException {
        Map<String, String> variables = new HashMap<>();

        if (environmentFile != null) {
            try (FileInputStream is = new FileInputStream(environmentFile)) {
                Properties prop = new Properties();
                prop.load(is);

                for (Object key : prop.keySet()) {
                    variables.put((String) key, prop.getProperty((String) key));
                }
            } catch (FileNotFoundException err) {
              getLog().error("File containing environment variables (configuration 'environmentFile') at '"
                + environmentFile.getAbsolutePath() + "' could not be found, skipping it.");
            } catch (IOException err) {
              throw new PreExecutionException("Trying to read file containing environment variables (configuration 'environmentFile') at '"
                + environmentFile.getAbsolutePath() + "' failed.", err);
            }
        }

        if (environmentVariables != null) {
            variables.putAll(environmentVariables);
        }

        return variables;
    }
}
