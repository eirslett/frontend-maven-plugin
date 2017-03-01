package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.LoggerFactory;


@Mojo(name="protractor",  defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public final class ProtractorRunMojo extends AbstractFrontendMojo {

    /**
     * Path to your protractor configuration file, relative to the working directory (default is "protractor.conf.js")
     */
    @Parameter(defaultValue = "protractor.conf.js", property = "protractorConfPath")
    private String protractorConfPath;

    /**
     * Whether you should continue build when some test will fail (default is false)
     */
    @Parameter(property = "maven.test.failure.ignore", required = false, defaultValue = "false")
    private Boolean testFailureIgnore;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.protractor", defaultValue = "false")
    private Boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        try {
            factory.getProtractorRunner().execute(protractorConfPath, environmentVariables);
        }
        catch (TaskRunnerException e) {
            if (testFailureIgnore) {
                LoggerFactory.getLogger(ProtractorRunMojo.class)
                        .warn("There are ignored test failures/errors for: " + workingDirectory);
            }
            else {
                throw e;
            }
        }
    }
}
