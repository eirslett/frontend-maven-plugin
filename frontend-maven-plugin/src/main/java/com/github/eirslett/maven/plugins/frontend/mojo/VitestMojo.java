package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name = "vitest", defaultPhase = LifecyclePhase.TEST, threadSafe = true)
public final class VitestMojo extends AbstractFrontendMojo {

    /**
     * Vitest arguments. Default is "run" so Maven builds do not start watch mode.
     */
    @Parameter(defaultValue = "run", property = "frontend.vitest.arguments")
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.vitest", defaultValue = "${skip.vitest}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        factory.getVitestRunner().execute(arguments, environmentVariables);
    }
}
