package com.github.eirslett.gradle.frontend

import com.github.eirslett.maven.plugins.frontend.lib.InstallationException
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory

public final class FrontendPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('install-node-and-npm') {
            File buildDir = project.getBuildDir()
            nodeVersion = project.frontend.nodeVersion
            npmVersion = project.frontend.npmVersion
            try {
                println buildDir
                new FrontendPluginFactory(buildDir).getNodeAndNPMInstaller().install(nodeVersion, npmVersion, 'http://nodejs.org/dist/')
            } catch (InstallationException e) {
                throw new GradleException(e.getLocalizedMessage())
            }
        }
    }
}
