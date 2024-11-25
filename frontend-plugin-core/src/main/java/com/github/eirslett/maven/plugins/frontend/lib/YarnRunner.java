package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;

public interface YarnRunner extends NodeTaskRunner {
}

final class DefaultYarnRunner extends YarnTaskExecutor implements YarnRunner {

    private static final String TASK_NAME = "yarn";

    public DefaultYarnRunner(YarnExecutorConfig config, ProxyConfig proxyConfig, String npmRegistryURL) {
        super(config, TASK_NAME, config.getYarnPath().getAbsolutePath(),
            buildArguments(config, proxyConfig, npmRegistryURL));
    }

    private static List<String> buildArguments(final YarnExecutorConfig config, ProxyConfig proxyConfig,
            String npmRegistryURL) {
        List<String> arguments = new ArrayList<>();

        if (config.isYarnBerry()) {
            // Yarn berry does not support the additional arguments we try to set below.
            // Setting those results in failures during yarn execution.
            return arguments;
        }

        if (npmRegistryURL != null && !npmRegistryURL.isEmpty()) {
            arguments.add("--registry=" + npmRegistryURL);
        }

        if (!proxyConfig.isEmpty()) {
            Proxy proxy = null;
            if (npmRegistryURL != null && !npmRegistryURL.isEmpty()) {
                proxy = proxyConfig.getProxyForUrl(npmRegistryURL);
            }

            if (proxy == null) {
                proxy = proxyConfig.getSecureProxy();
            }

            if (proxy == null) {
                proxy = proxyConfig.getInsecureProxy();
            }

            arguments.add("--https-proxy=" + proxy.getUri().toString());
            arguments.add("--proxy=" + proxy.getUri().toString());
        }

        return arguments;
    }

    /**
     * Running {@code yarn versions} gives an output like this:
     * <pre>
     * yarn versions v1.22.22
     * {
     *   yarn: '1.22.22',
     *   node: '18.17.0',
     *   acorn: '8.8.2',
     *   ada: '2.5.0',
     *   ....
     * }
     * Done in 0.01s.
     * </pre>
     * The first line is repeated and the last one will cause an unnecessary diff. Running
     * with {@code --silent culls the first and last lines}, but this isn't available on all yarn
     * classic versions, e.g. 1.22.17
     */
    @Override
    public Optional<Runtime> getRuntime() {
        try {
            String rawVersions = new YarnExecutor(config, singletonList("versions"), emptyMap())
                    .executeAndGetResult(logger);
            int startIndex = rawVersions.indexOf("{");
            int endIndex = rawVersions.indexOf("}") + 1;
            String desiredVersions = rawVersions.substring(startIndex, endIndex);
            // Yes, yarn is not a runtime, but here we can glean a little more
            // information that's more ideal to track
            return Optional.of(new Runtime("yarn", desiredVersions));
        } catch (Exception exception) {
            logger.debug("Failed to get the runtime versions from yarn, because: ", exception);
            return empty();
        }
    }
}
