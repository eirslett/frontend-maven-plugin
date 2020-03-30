package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

public abstract class AbstractFrontendMojo extends AbstractMojo {

    public static final String INTEGRATION_TEST_FAILED_KEY = "integrationTestFailed";
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
     * Set this to true to delegate test failures until after 'post-integration' has executed.
     *
     * @since 1.9.2
     */
    @Parameter(property = "maven.it.failure.after.postIntegration", defaultValue = "false")
    protected boolean integrationTestFailureAfterPostIntegration;

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
     * Additional environment variables to pass to the build.
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
        return "test".equals(phase) || isIntegrationTestingPhase();
    }

    private boolean isIntegrationTestingPhase(){
        String phase = execution.getLifecyclePhase();
        return "integration-test".equals(phase);
    }

    private boolean isVerifyPhase(){
        String phase = execution.getLifecyclePhase();
        return "verify".equals(phase);
    }

    protected abstract void execute(FrontendPluginFactory factory) throws FrontendException;

    /**
     * Implemented by children to determine if this execution should be skipped.
     */
    protected abstract boolean skipExecution();

    @Override
    public void execute() throws MojoFailureException {
        if(isVerifyPhase() && integrationTestsHaveFailed()){
            throw new MojoFailureException("Some integration tests have failed during integration-test phase.");
        }
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
                } else if (integrationTestFailureAfterPostIntegration && isIntegrationTestingPhase()) {
                    storeIntegrationTestFailed();
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

    private Boolean integrationTestsHaveFailed() {
        Object failed = getPluginContext().get(INTEGRATION_TEST_FAILED_KEY);
        return failed != null && (Boolean) failed;
    }

    private void storeIntegrationTestFailed() {
        Map<String, Object> pluginContext;
        if(getPluginContext() != null){
            pluginContext = getPluginContext();
        } else {
            pluginContext = new HashMap<>();
        }
        pluginContext.put(INTEGRATION_TEST_FAILED_KEY, true);
        setPluginContext(pluginContext);
    }

}
