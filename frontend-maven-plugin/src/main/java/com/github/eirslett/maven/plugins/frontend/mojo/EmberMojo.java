package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;

import static com.google.common.base.Strings.isNullOrEmpty;

@Mojo(name = "ember", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class EmberMojo extends AbstractMojo {
    private static final String TEST_COMMAND = "test";

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(property = "workingDirectory", defaultValue = "${basedir}", required = false)
    private File workingDirectory;

    /**
     * The ember-cli command to execute.
     */
    @Parameter(property = "command", required = true)
    private String command;

    /**
     * Additional arguments passed to the specified {@link EmberMojo#command}
     */
    @Parameter(property = "arguments", required = false)
    private String arguments;

    /**
     * The directory where ember-cli places the generated project files. This option depends
     * on your ember-cli arguments. You should define this if you want to notify Eclipse about changes.
     */
    @Parameter(property = "outputPath", required = false)
    private File outputPath;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.ember", defaultValue = "false")
    private Boolean skip;

    /**
     * Skips execution of ember test command
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    private Boolean skipTests;

    @Component
    private BuildContext buildContext;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping ember execution");
            return;
        } else if (TEST_COMMAND.equals(command) && skipTests) {
            getLog().info("Skipping ember test execution");
            return;
        }

        try {
            MojoUtils.setSLF4jLogger(getLog());
            new FrontendPluginFactory(workingDirectory)
                    .getEmberRunner()
                    .execute(getArguments());
            if (outputPath != null) {
                buildContext.refresh(outputPath);
            }
        } catch (TaskRunnerException e) {
            throw new MojoFailureException("Failed to run task", e);
        }
    }

    private String getArguments() {
        String commandLine = command;
        if (!isNullOrEmpty(arguments)) {
            commandLine += " " + arguments;
        }

        return commandLine;
    }
}