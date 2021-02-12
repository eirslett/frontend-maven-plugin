package com.github.eirslett.maven.plugins.frontend.mojo;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mojo(name="npx",  defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public final class NpxMojo extends AbstractFrontendMojo {

    private static final String NPM_REGISTRY_URL = "npmRegistryURL";
    
    /**
     * npx arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.npx.arguments", required = false)
    private String arguments;

    @Parameter(property = "frontend.npx.npmInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean npmInheritsProxyConfigFromMaven;

    /**
     * Registry override, passed as the registry option during npm install if set.
     */
    @Parameter(property = NPM_REGISTRY_URL, required = false, defaultValue = "")
    private String npmRegistryURL;
    
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component
    private BuildContext buildContext;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Files that should be checked for changes, in addition to the srcdir files.
     * Defaults to package.json in the {@link #workingDirectory}.
     */
    @Parameter(property = "triggerfiles")
    private List<File> triggerfiles;

    /**
     * The directory containing front end files that will be processed by npx.
     * If this is set then files in the directory will be checked for
     * modifications before running npx.
     */
    @Parameter(property = "srcdir")
    private File srcdir;

    /**
     * The directory where front end files will be output by npx. If this is
     * set then they will be refreshed so they correctly show as modified in
     * Eclipse.
     */
    @Parameter(property = "outputdir")
    private File outputdir;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.npx", defaultValue = "${skip.npx}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        if (shouldExecute()) {
            ProxyConfig proxyConfig = getProxyConfig();
            factory.getNpxRunner(proxyConfig, getRegistryUrl()).execute(arguments, environmentVariables);

            if (outputdir != null) {
                getLog().info("Refreshing files after 'npx" + (arguments != null ? " " + arguments : "") + "': " + outputdir);
                buildContext.refresh(outputdir);
            }
        } else {
            getLog().info("Skipping 'npx" + (arguments != null ? " " + arguments : "") + "' as "
                    + triggerfiles.stream().map(Object::toString).collect(Collectors.joining(", "))
                    + " unchanged" + (srcdir != null ? " and no modified files in: " + srcdir : ""));
        }
    }

    private boolean shouldExecute() {
        if (triggerfiles == null || triggerfiles.isEmpty()) {
            triggerfiles = Arrays.asList(new File(workingDirectory, "package.json"));
        }

        return MojoUtils.shouldExecute(buildContext, triggerfiles, srcdir);
    }

    private ProxyConfig getProxyConfig() {
        if (npmInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(session, decrypter);
        } else {
            getLog().info("npm not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        }
    }

    private String getRegistryUrl() {
        // check to see if overridden via `-D`, otherwise fallback to pom value
        return System.getProperty(NPM_REGISTRY_URL, npmRegistryURL);
    }
}
