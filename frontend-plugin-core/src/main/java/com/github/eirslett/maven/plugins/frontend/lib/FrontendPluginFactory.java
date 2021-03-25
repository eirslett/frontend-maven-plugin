package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

public final class FrontendPluginFactory {
    
    private static final Platform defaultPlatform = Platform.guess();
    private static final String DEFAULT_CACHE_PATH = "cache";

    private final File workingDirectory;
    private final File installDirectory;
    private final CacheResolver cacheResolver;

    public FrontendPluginFactory(File workingDirectory, File installDirectory){
        this(workingDirectory, installDirectory, getDefaultCacheResolver(installDirectory));
    }

    public FrontendPluginFactory(File workingDirectory, File installDirectory, CacheResolver cacheResolver){
        this.workingDirectory = workingDirectory;
        this.installDirectory = installDirectory;
        this.cacheResolver = cacheResolver;
    }

    public NodeInstaller getNodeInstaller(ProxyConfig proxy) {
        return new NodeInstaller(getInstallConfig(), new DefaultArchiveExtractor(), new DefaultFileDownloader(proxy));
    }

    public NPMInstaller getNPMInstaller(ProxyConfig proxy) {
        return new NPMInstaller(getInstallConfig(), new DefaultArchiveExtractor(), new DefaultFileDownloader(proxy));
    }

    public PNPMInstaller getPNPMInstaller(ProxyConfig proxy) {
        return new PNPMInstaller(getInstallConfig(), new DefaultArchiveExtractor(), new DefaultFileDownloader(proxy));
    }

    public YarnInstaller getYarnInstaller(ProxyConfig proxy) {
        return new YarnInstaller(getInstallConfig(), new DefaultArchiveExtractor(), new DefaultFileDownloader(proxy));
    }
    
    public BowerRunner getBowerRunner(ProxyConfig proxy) {
        return new DefaultBowerRunner(getExecutorConfig(), proxy);
    }    

    public JspmRunner getJspmRunner() {
        return new DefaultJspmRunner(getExecutorConfig());
    }

    public NpmRunner getNpmRunner(ProxyConfig proxy, String npmRegistryURL) {
        return new DefaultNpmRunner(getExecutorConfig(), proxy, npmRegistryURL);
    }

    public PnpmRunner getPnpmRunner(ProxyConfig proxyConfig, String npmRegistryUrl) {
        return new DefaultPnpmRunner(getExecutorConfig(), proxyConfig, npmRegistryUrl);
    }

    public NpxRunner getNpxRunner(ProxyConfig proxy, String npmRegistryURL) {
        return new DefaultNpxRunner(getExecutorConfig(), proxy, npmRegistryURL);
    }

    public YarnRunner getYarnRunner(ProxyConfig proxy, String npmRegistryURL) {
        return new DefaultYarnRunner(new InstallYarnExecutorConfig(getInstallConfig()), proxy, npmRegistryURL);
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
        return new DefaultInstallConfig(installDirectory, workingDirectory, cacheResolver, defaultPlatform);
    }

    private static final CacheResolver getDefaultCacheResolver(File root) {
        return new DirectoryCacheResolver(new File(root, DEFAULT_CACHE_PATH));
    }
}
