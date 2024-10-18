package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.naming.OperationNotSupportedException;

import static java.util.Objects.requireNonNull;

public class AtlassianUtil {
    private AtlassianUtil() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("my dude, this is a util class");
    }

    public static boolean isAtlassianProject(@Nonnull MavenProject mavenProject) {
        requireNonNull(mavenProject, "mavenProject");

        // Ordered by likelihood
        if (containsAtlassian(mavenProject.getGroupId()) ||
            containsAtlassian(mavenProject.getVersion()) || // we have forks with original coordinates
                containsAtlassian(mavenProject.getArtifactId())) {
            return true;
        }

        // I don't think I'm missing anything here for forks that have a frontend??
        // AO plugin, Greenhopper, and Ridalabs all have Atlassian group IDs now
        // Please take mercy on me and don't be offended if I forgot something

        return false;
    }

    private static boolean containsAtlassian(@Nonnull String string) {
        requireNonNull(string, "string");

        return string.toLowerCase().contains("atlassian");
    }
}
