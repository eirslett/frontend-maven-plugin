package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.LoggerFactory;


@Mojo(name="karma",  defaultPhase = LifecyclePhase.TEST)
public final class KarmaRunMojo extends AbstractFrontendMojo {

    /**
     * Path to your karma configuration file, relative to the working directory (default is "karma.conf.js")
     */
    @Parameter(defaultValue = "karma.conf.js", property = "karmaConfPath")
    private String karmaConfPath;

    /**
     * Whether you should skip running the tests (default is false)
     */
    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    private Boolean skipTests;

    /**
     * Whether you should continue build when some test will fail (default is false)
     */
    @Parameter(property = "testFailureIgnore", required = false, defaultValue = "false")
    private Boolean testFailureIgnore;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.karma", defaultValue = "false")
    private Boolean skip;

    @Override
    protected boolean isSkipped() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        try {
            if (skipTests) {
                LoggerFactory.getLogger(KarmaRunMojo.class).info("Skipping karma tests.");
            }
            else {
                factory.getKarmaRunner().execute("start " + karmaConfPath);
            }
        }
        catch (TaskRunnerException e) {
            if (testFailureIgnore) {
                LoggerFactory.getLogger(KarmaRunMojo.class)
                        .warn("There are ignored test failures/errors for: " + workingDirectory);
            }
            else {
                throw e;
            }
        }
    }
}
