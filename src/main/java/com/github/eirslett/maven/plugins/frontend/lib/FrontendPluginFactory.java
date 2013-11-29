package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import java.io.File;

public final class FrontendPluginFactory {
    private static final Logger defaultLogger = NOPLogger.NOP_LOGGER;
    private static final Platform defaultPlatform = Platform.guess();

    private FrontendPluginFactory(){}

    public static NodeAndNPMInstaller getNodeAndNPMInstaller(File workingDirectory){
        return getNodeAndNPMInstaller(workingDirectory, defaultLogger);
    }

    public static NodeAndNPMInstaller getNodeAndNPMInstaller(File workingDirectory, Logger logger){
        return new DefaultNodeAndNPMInstaller(
                workingDirectory,
                logger,
                defaultPlatform,
                new DefaultArchiveExtractor(),
                new DefaultFileDownloader());
    }

    public static NpmRunner getNpmRunner(File workingDirectory){
        return getNpmRunner(workingDirectory, defaultLogger);
    }

    public static NpmRunner getNpmRunner(File workingDirectory, Logger logger) {
        return new DefaultNpmRunner(logger, defaultPlatform, workingDirectory);
    }

    public static GruntRunner getGruntRunner(File workingDirectory){
        return getGruntRunner(workingDirectory, defaultLogger);
    }

    public static GruntRunner getGruntRunner(File workingDirectory, Logger logger){
        return new DefaultGruntRunner(logger, defaultPlatform, workingDirectory);
    }

    public static KarmaRunner getKarmaRunner(File workingDirectory){
        return getKarmaRunner(workingDirectory, defaultLogger);
    }

    public static KarmaRunner getKarmaRunner(File workingDirectory, Logger logger){
        return new DefaultKarmaRunner(logger, defaultPlatform, workingDirectory);
    }
}
