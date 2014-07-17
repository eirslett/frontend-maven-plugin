package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.util.Properties;

public final class FrontendPluginFactory {
    private static final Platform defaultPlatform = Platform.guess();
    private final File workingDirectory;
    private final ProxyConfig proxy;
    private final Properties environmentVariables;

    public FrontendPluginFactory(File workingDirectory){
        this(workingDirectory, null);
    }
    public FrontendPluginFactory(File workingDirectory, ProxyConfig proxy){
        this.workingDirectory = workingDirectory;
        this.proxy = proxy;
        this.environmentVariables = new Properties();

    }

    public FrontendPluginFactory(File workingDirectory, ProxyConfig proxy, Properties environmentVariables){
        this.workingDirectory = workingDirectory;
        this.proxy = proxy;
        this.environmentVariables = environmentVariables;
    }

    public NodeAndNPMInstaller getNodeAndNPMInstaller(){
        return new DefaultNodeAndNPMInstaller(
                workingDirectory,
                defaultPlatform,
                new DefaultArchiveExtractor(),
                new DefaultFileDownloader(proxy));
    }

    public NpmRunner getNpmRunner() {
        return new DefaultNpmRunner(defaultPlatform, workingDirectory, proxy, this.environmentVariables);
    }

    public GruntRunner getGruntRunner(){
        return new DefaultGruntRunner(defaultPlatform, workingDirectory);
    }

    public KarmaRunner getKarmaRunner(){
        return new DefaultKarmaRunner(defaultPlatform, workingDirectory);
    }

    public GulpRunner getGulpRunner(){
        return new DefaultGulpRunner(defaultPlatform, workingDirectory);
    }
}
