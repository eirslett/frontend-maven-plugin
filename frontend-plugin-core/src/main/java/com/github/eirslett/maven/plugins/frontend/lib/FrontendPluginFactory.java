package com.github.eirslett.maven.plugins.frontend.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.io.File;

public final class FrontendPluginFactory {
    private static final Platform defaultPlatform = Platform.guess();

    private FrontendPluginFactory(){}

    public static NodeAndNPMInstaller getNodeAndNPMInstaller(File workingDirectory){
        return getNodeAndNPMInstaller(workingDirectory, getDefaultLogger());
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
        return getNpmRunner(workingDirectory, getDefaultLogger());
    }

    public static NpmRunner getNpmRunner(File workingDirectory, Logger logger) {
        return new DefaultNpmRunner(logger, defaultPlatform, workingDirectory);
    }

    public static GruntRunner getGruntRunner(File workingDirectory){
        return getGruntRunner(workingDirectory, getDefaultLogger());
    }

    public static GruntRunner getGruntRunner(File workingDirectory, Logger logger){
        return new DefaultGruntRunner(logger, defaultPlatform, workingDirectory);
    }

    public static KarmaRunner getKarmaRunner(File workingDirectory){
        return getKarmaRunner(workingDirectory, getDefaultLogger());
    }

    public static KarmaRunner getKarmaRunner(File workingDirectory, Logger logger){
        return new DefaultKarmaRunner(logger, defaultPlatform, workingDirectory);
    }

    private static Logger getDefaultLogger(){
        return LoggerFactory.getLogger("Frontend Plugin");
    }
}
