package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public final class FrontendPluginFactory {
    private static final Platform defaultPlatform = Platform.guess();
    private final File workingDirectory;
    private final ProxyConfig proxy;

    public FrontendPluginFactory(File workingDirectory){
        this(workingDirectory, null);
    }
    public FrontendPluginFactory(File workingDirectory, ProxyConfig proxy){
        this.workingDirectory = workingDirectory;
        this.proxy = proxy;
    }

    public NodeAndNPMInstaller getNodeAndNPMInstaller(){
        return new DefaultNodeAndNPMInstaller(
                workingDirectory,
                defaultPlatform,
                new DefaultArchiveExtractor(),
                new DefaultFileDownloader(proxy));
    }

    public NpmRunner getNpmRunner() {
        return new DefaultNpmRunner(defaultPlatform, workingDirectory, proxy);
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

    public GrocRunner getGrocRunner(){
        return new DefaultGrocRunner(defaultPlatform, workingDirectory);
    }

    public ProtractorRunner getProtractorRunner(){ return new DefaultProtractorRunner(defaultPlatform, workingDirectory);}

    public SeleniumStandaloneRunner getSeleniumRunner(){ return new DefaultSeleniumStandaloneRunner(defaultPlatform,
            workingDirectory);}

    public WebdriverManagerRunner getWebdriverManagerRunner(){ return new DefaultWebdriverManagerRunner(defaultPlatform, workingDirectory);}

    public WebdriverManagerUpdater getWebdriverManagerUpdater(){ return new DefaultWebdriverManagerUpdater(defaultPlatform,
            workingDirectory);}
}
