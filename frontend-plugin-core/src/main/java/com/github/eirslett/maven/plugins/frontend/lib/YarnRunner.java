package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig.Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
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
     * <h3>Why not use? / Why not do this instead?</h3>
     * <ul>
     *     <li><i>Execute node/yarn from the path?</i>It's often different to the
     *     Node version used by this plugin because people often have to mix
     *     versions AND because this plugin executes all tasks from the version
     *     it downloaded</li>
     *     <li><i>Use the same command between yarn versions?</i>There is
     *     no command compatible with all version of yarn we use and support</li>
     *     <li><i>Use yarn to call node?</i>In Yarn classic it's not a reserved
     *     command which can lead to crashes if a project has a "node"
     *     script (reasonably common)</li>
     * </ul>
     * <h3>Output of the commands</h3>
     * <h5>In Yarn Berry</h5>
     * <p>
     * Running {@code yarn node --version} in yarn classic gives us an output
     * like this of all the C++ libraries and the package name in the
     * {@code package.json}:
     * <pre>
     * yarn node v1.22.22
     * v20.10.0
     * Done in 0.02s.
     * </pre>
     * while yarn berry will give us just the output
     * </p>
     * <h5>In Yarn Classic</h5>
     * <p>
     * Running {@code yarn versions} gives an output like this:
     * <pre>
     * yarn versions v1.22.22
     * {
     *   yarn: '1.22.22',
     *   '@atlassian/aui-workspace': '9.13.0-SNAPSHOT',
     *   node: '18.17.0',
     *   acorn: '8.8.2',
     *   ada: '2.5.0',
     *   ....
     * }
     * Done in 0.01s.
     * </pre>
     * The first line is repeated and the last one will cause an unnecessary diff. Running
     * with {@code --silent} culls the first and last lines, but this isn't available on all yarn
     * classic versions, e.g. 1.22.17
     * </p>
     */
    @Override
    public Optional<Runtime> getRuntime() {
        // Why not call config.isYarnBerry() ?? Well it can lie since it's not using what's
        // on the path..... it's just looking at files
        try {
            String output = new YarnExecutor(config, asList("node", "--version"), emptyMap())
                    .executeAndGetResult(logger);
            if (output.startsWith("yarn node")) {
                output = output.substring(output.indexOf("\n"), output.lastIndexOf("\n")).trim();
            }
            return Optional.of(new Runtime("node", output));
        } catch (Exception exception) {
            logger.debug("Failed to get the Node version from yarn, will fallback and hope it's yarn classic. Failed because: ", exception);
        }

        try {
            String rawVersions = new YarnExecutor(config, singletonList("versions"), emptyMap())
                    .executeAndGetResult(logger);
            int startIndex = rawVersions.indexOf("{");
            int endIndex = rawVersions.indexOf("}") + 1;
            String desiredVersions = rawVersions.substring(startIndex, endIndex)
                    .replaceAll("\\s+", "");
            // Yes, yarn is not a runtime, but here we can glean a little more
            // information that's more ideal to track
            return Optional.of(new Runtime("yarn", desiredVersions));
        } catch (Exception exception) {
            logger.debug("Failed to get the Node version from yarn, even after assuming it's yarn classic. Failed because: ", exception);
            return empty();
        }
    }
}
