package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.jackson.map.ObjectMapper;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

@Mojo(name="package-json",  defaultPhase = LifecyclePhase.VALIDATE)
public final class PackageJsonMojo extends AbstractFrontendMojo {

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.npm", defaultValue = "false")
    private Boolean skip;
    
    /**
     * The maven project.
     */
    @Component
    MavenProject project;

    @Override
    protected boolean isSkipped() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        File packageJson = new File(workingDirectory, "package.json");
        try {
            Properties projectProps = project.getProperties();
            @SuppressWarnings("unchecked")
            Map<String, Object> props = new ObjectMapper().readValue(packageJson, Map.class);
            copyProperty("name", props, projectProps);
            copyProperty("version", props, projectProps);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse package.json", e);
        }
    }
    
    private void copyProperty(String name, Map<String, Object> props, Properties projectProps) {
      if (props.containsKey(name)) {
          projectProps.setProperty("package.json." + name, props.get(name).toString());
      }
    }
}
