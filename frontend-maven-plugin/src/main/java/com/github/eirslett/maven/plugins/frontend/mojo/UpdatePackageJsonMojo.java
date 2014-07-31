package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import static com.github.eirslett.maven.plugins.frontend.mojo.MojoUtils.setSLF4jLogger;
import static com.github.eirslett.maven.plugins.frontend.mojo.ProjectInfoUtils.convert;
import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "update-packge-json", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class UpdatePackageJsonMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    private File workingDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            setSLF4jLogger(getLog());
            new FrontendPluginFactory(workingDirectory)
                    .getPackageJsonUpdater()
                    .update(convert(project));
        } catch (TaskRunnerException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

}
