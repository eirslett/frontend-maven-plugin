package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public final class FrontendPluginFactory {
    private static final Platform defaultPlatform = Platform.guess();
    private final File workingDirectory;
    private final ProxyConfig proxy;
	private final boolean local;

    public FrontendPluginFactory(File workingDirectory, boolean local){
        this(workingDirectory, null, local);
    }
    public FrontendPluginFactory(File workingDirectory, ProxyConfig proxy, boolean local){
        this.workingDirectory = workingDirectory;
        this.proxy = proxy;
		this.local = local;
    }

    public NodeAndNPMInstaller getNodeAndNPMInstaller(){
        return new DefaultNodeAndNPMInstaller(
                workingDirectory,
                defaultPlatform,
                new DefaultArchiveExtractor(),
                new DefaultFileDownloader(proxy));
    }

    public NpmRunner getNpmRunner() {
        return local ? new LocalNpmRunner(defaultPlatform, workingDirectory) : new DefaultNpmRunner(defaultPlatform, workingDirectory, proxy);
    }
    
    public GruntRunner getGruntRunner(){
        return new DefaultGruntRunner(defaultPlatform, workingDirectory, local);
    }

    public KarmaRunner getKarmaRunner(){
        return new DefaultKarmaRunner(defaultPlatform, workingDirectory, local);
    }

    public GulpRunner getGulpRunner(){
        return new DefaultGulpRunner(defaultPlatform, workingDirectory, local);
    }
}
