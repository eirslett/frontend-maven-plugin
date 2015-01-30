package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public final class FrontendPluginFactory {
    private static final Platform defaultPlatform = Platform.guess();
    private final File nodeInstallDirectory;
    private final File workingDirectory;
    private final ProxyConfig proxy;

    public FrontendPluginFactory(File nodeInstallDirectory, File workingDirectory){
        this(nodeInstallDirectory, workingDirectory, null);
    }
    public FrontendPluginFactory(File nodeInstallDirectory, File workingDirectory, ProxyConfig proxy){
        this.nodeInstallDirectory = nodeInstallDirectory;
        this.workingDirectory = workingDirectory;
        this.proxy = proxy;
    }

    public NodeAndNPMInstaller getNodeAndNPMInstaller(){
        return new DefaultNodeAndNPMInstaller(
                nodeInstallDirectory,
                workingDirectory,
                defaultPlatform,
                new DefaultArchiveExtractor(),
                new DefaultFileDownloader(proxy));
    }
    
    public BowerRunner getBowerRunner() {
        return new DefaultBowerRunner(defaultPlatform, nodeInstallDirectory, workingDirectory);
    }    

    public NpmRunner getNpmRunner() {
        return new DefaultNpmRunner(defaultPlatform, nodeInstallDirectory, workingDirectory, proxy);
    }

    public GruntRunner getGruntRunner(){
        return new DefaultGruntRunner(defaultPlatform, nodeInstallDirectory, workingDirectory);
    }

    public KarmaRunner getKarmaRunner(){
        return new DefaultKarmaRunner(defaultPlatform, nodeInstallDirectory, workingDirectory);
    }

    public GulpRunner getGulpRunner(){
        return new DefaultGulpRunner(defaultPlatform, nodeInstallDirectory, workingDirectory);
    }
}
