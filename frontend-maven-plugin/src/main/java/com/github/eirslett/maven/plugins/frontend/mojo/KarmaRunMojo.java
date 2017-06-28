package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name="karma",  defaultPhase = LifecyclePhase.TEST)
public final class KarmaRunMojo extends AbstractFrontendMojo {

    /**
     * Path to your karma configuration file, relative to the working directory (default is "karma.conf.js")
     */
    @Parameter(defaultValue = "karma.conf.js", property = "karmaConfPath")
    private String karmaConfPath;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.karma", defaultValue = "${skip.karma}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        factory.getKarmaRunner().execute("start " + karmaConfPath, environmentVariables);
    }
}
