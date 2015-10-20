package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

@Mojo(name="tsc", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class TypescriptMojo extends AbstractFrontendMojo {

	/**
	 * The name of the file in which the args for the typescript-compiler should be
	 * stored. See "tsc --help" -> "tsc @args.txt"
	 */
	@Parameter(property = "args.txt", defaultValue = "target/tsc-args.txt")
	private File argsTxt;
	
    /**
     * The directory containing typescript files that will be processed by tsc.
     */
    @Parameter(property = "srcdir")
    private File srcdir;

    /**
     * The directory where compiled typescript files will be output by tsc. If this is
     * set then they will be refreshed so they correctly show as modified in
     * Eclipse.
     */
    @Parameter(property = "outputdir")
    private File outputdir;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.tsc", defaultValue = "false")
    private Boolean skip;

    @Component
    private BuildContext buildContext;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {

        factory.getTypescriptRunner().execute(argsTxt, srcdir, outputdir);

        if (outputdir != null) {
            getLog().info("Refreshing files after tsc: " + outputdir);
            buildContext.refresh(outputdir);
        }
        
    }

}
