package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "brunch", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class BrunchMojo extends AbstractFrontendMojo {

    /**
     * Brunch arguments. Default is empty (runs just the "brunch" command).
     */
    @Parameter(property = "frontend.brunch.arguments")
    private String arguments;

    /**
     * The directory containing front end files that will be processed by brunch.
     * If this is set then files in the directory will be checked for
     * modifications before running brunch.
     */
    @Parameter(property = "srcdir")
    private File srcdir;

    /**
     * Files that should be checked for changes, in addition to the srcdir files.
     * Defaults to config.coffee in the {@link #workingDirectory}.
     */
    @Parameter(property = "triggerfiles")
    private List<File> triggerfiles;

    /**
     * The directory where front end files will be output by brunch. If this is
     * set then they will be refreshed so they correctly show as modified in
     * Eclipse.
     */
    @Parameter(property = "outputdir")
    private File outputdir;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.brunch", defaultValue = "false")
    private Boolean skip;

    @Component
    private BuildContext buildContext;

    @Override
    protected boolean isSkipped() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        if (shouldExecute()) {
            if (arguments == null || arguments.isEmpty()) {
                arguments = "build --production";
            }
            getLog().debug("Execute brunch with arguments: " + arguments);
            factory.getBrunchRunner().execute(arguments);

            if (outputdir != null) {
                getLog().info("Refreshing files after brunch: " + outputdir);
                buildContext.refresh(outputdir);
            }
        } else {
            getLog().info("Skipping brunch as no modified files in " + srcdir);
        }
    }

    private boolean shouldExecute() {
        if (triggerfiles == null || triggerfiles.isEmpty()) {
            triggerfiles = Arrays.asList(new File(workingDirectory, "config.coffee"));
        }

        return MojoUtils.shouldExecute(buildContext, triggerfiles, srcdir);
    }

}
