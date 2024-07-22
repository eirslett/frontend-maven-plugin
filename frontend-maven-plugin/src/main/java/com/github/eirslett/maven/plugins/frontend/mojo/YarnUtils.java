package com.github.eirslett.maven.plugins.frontend.mojo;

import java.io.File;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;

public class YarnUtils {

    private static final String YARNRC_YAML_FILE_NAME = ".yarnrc.yml";

    /**
     * Checks whether a .yarnrc.yml file exists at the project root
     * (in multi-module builds, it will be the Reactor project)
     *
     * @param session
     *        the current maven session
     * @param workingDirectory
     *        the configured working directory
     *
     * @return true if the .yarnrc.yml file exists, false otherwise
     */
    public static boolean isYarnrcYamlFilePresent(MavenSession session, File workingDirectory) {
        Stream<File> filesToCheck = Stream.of(
                new File(session.getCurrentProject().getBasedir(), YARNRC_YAML_FILE_NAME),
                new File(session.getRequest().getMultiModuleProjectDirectory(), YARNRC_YAML_FILE_NAME),
                new File(session.getExecutionRootDirectory(), YARNRC_YAML_FILE_NAME),
                new File(workingDirectory, YARNRC_YAML_FILE_NAME)
        );

        return filesToCheck
                .anyMatch(File::exists);
    }
}
