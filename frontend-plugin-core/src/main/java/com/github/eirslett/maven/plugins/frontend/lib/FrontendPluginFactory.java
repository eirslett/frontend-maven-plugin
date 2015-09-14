package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public final class FrontendPluginFactory {
    private static final Platform defaultPlatform = Platform.guess();

    private final File workingDirectory;
    private final File installDirectory;

    public FrontendPluginFactory(File workingDirectory, File installDirectory){
        this.workingDirectory = workingDirectory;
        this.installDirectory = installDirectory;
    }

    public NodeAndNPMInstaller getNodeAndNPMInstaller(ProxyConfig proxy){
        return new DefaultNodeAndNPMInstaller(
                getInstallConfig(),
                new DefaultArchiveExtractor(),
                new DefaultFileDownloader(proxy));
    }
    
    public BowerRunner getBowerRunner() {
        return new DefaultBowerRunner(getExecutorConfig());
    }    

    public JspmRunner getJspmRunner() {
        return new DefaultJspmRunner(getExecutorConfig());
    }

    public NpmRunner getNpmRunner(ProxyConfig proxy) {
        return new DefaultNpmRunner(getExecutorConfig(), proxy);
    }

    public GruntRunner getGruntRunner(){
        return new DefaultGruntRunner(getExecutorConfig());
    }

    public EmberRunner getEmberRunner() {
        return new DefaultEmberRunner(getExecutorConfig());
    }

    public KarmaRunner getKarmaRunner(){
        return new DefaultKarmaRunner(getExecutorConfig());
    }

    public GulpRunner getGulpRunner(){
        return new DefaultGulpRunner(getExecutorConfig());
    }

    public WebpackRunner getWebpackRunner(){
        return new DefaultWebpackRunner(getExecutorConfig());
    }

    private NodeExecutorConfig getExecutorConfig() {
        return new InstallNodeExecutorConfig(getInstallConfig());
    }

    private InstallConfig getInstallConfig() {
        return new DefaultInstallConfig(installDirectory, workingDirectory, defaultPlatform);
    }
}
