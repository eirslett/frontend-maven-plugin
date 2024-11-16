package com.github.eirslett.maven.plugins.frontend.lib;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricsReporter.incrementCount;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.NodeVersionLocations.MAVEN;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.NodeVersionLocations.MISE;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.NodeVersionLocations.NODE_VERSION;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.NodeVersionLocations.NVMRC;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.NodeVersionLocations.TOOL_VERSIONS;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.NodeVersionLocations.UNKNOWN;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.getDownloadableVersion;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.validateVersion;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

public class NodeVersionDetector {

    private static final Logger logger = getLogger(NodeVersionDetector.class);
    private static final String TOOL_VERSIONS_FILENAME = ".tool-versions";

    public static String getNodeVersion(
            File workingDir,
            String providedNodeVersion,
            String genericNodeVersionFile,
            String artifactId,
            String forkVersion
    ) throws Exception {
        final EventData eventData = new EventData(artifactId, forkVersion);

        if (!isNull(providedNodeVersion) && !providedNodeVersion.trim().isEmpty()) {
            logger.debug("Looks like a node version was set so using that: {}", providedNodeVersion);
            reportFoundVersion(MAVEN, providedNodeVersion, eventData);
            return providedNodeVersion;
        }

        if (!isNull(genericNodeVersionFile) && !genericNodeVersionFile.trim().isEmpty()) {
            File genericNodeVersionFileFile = new File(genericNodeVersionFile);
            if (!genericNodeVersionFileFile.exists()) {
                throw new Exception("The Node version file doesn't seem to exist: " + genericNodeVersionFileFile);
            }

            if (genericNodeVersionFile.endsWith(".toml") && genericNodeVersionFile.contains("mise")) {
                reportFoundVersion(MISE, providedNodeVersion, eventData);
                return readMiseConfigTomlFile(genericNodeVersionFileFile, genericNodeVersionFileFile.toPath());
            } else if (genericNodeVersionFile.endsWith(TOOL_VERSIONS_FILENAME)) {
                reportFoundVersion(TOOL_VERSIONS, providedNodeVersion, eventData);
                return readToolVersionsFile(genericNodeVersionFileFile, genericNodeVersionFileFile.toPath());
            } else {
                String versionLocation = genericNodeVersionFile.contains(NVMRC)
                        ? NVMRC
                        : genericNodeVersionFile.contains(NODE_VERSION)
                            ? NODE_VERSION
                            : UNKNOWN;
                reportFoundVersion(versionLocation, providedNodeVersion, eventData);
                return readNvmrcFile(genericNodeVersionFileFile, genericNodeVersionFileFile.toPath());
            }
        }

        try {
            return recursivelyFindVersion(workingDir, eventData);
        } catch (Throwable throwable) {
            logger.debug("Going to use the configuration node version, failed to find a file with the version because",
                    throwable);
            reportFoundVersion(MAVEN, providedNodeVersion, eventData);
            return providedNodeVersion;
        }
    }

    /**
     * Mise has way too many options, see:
     * <a href="https://mise.jdx.dev/profiles.html">https://mise.jdx.dev/profiles.html</a>
     * <a href="https://mise.jdx.dev/configuration.html#mise-toml">https://mise.jdx.dev/configuration.html#mise-toml</a>
     */
    public static List<String> listMiseConfigFilenames() {
        final String miseConfigDir = System.getenv("MISE_CONFIG_DIR");
        final String miseEnv = System.getenv("MISE_ENV");

        // The order is important and should respect mises' ordering
        final List<String> allMiseConfigFilenames = new ArrayList<>();

        allMiseConfigFilenames.add(format("%s/config.%s.toml", miseConfigDir, miseEnv));
        allMiseConfigFilenames.add(format("%s/mise.%s.toml", miseConfigDir, miseEnv));

        allMiseConfigFilenames.add(".config/mise/config.toml");
        allMiseConfigFilenames.add("mise/config.toml");
        allMiseConfigFilenames.add("mise.toml");
        allMiseConfigFilenames.add(".mise/config.toml");
        allMiseConfigFilenames.add(".mise.toml");
        allMiseConfigFilenames.add(".config/mise/config.local.toml");
        allMiseConfigFilenames.add("mise/config.local.toml");
        allMiseConfigFilenames.add("mise.local.toml");
        allMiseConfigFilenames.add(".mise/config.local.toml");
        allMiseConfigFilenames.add(".mise.local.toml");

        allMiseConfigFilenames.add(format(".config/mise/config.%s.toml", miseEnv));
        allMiseConfigFilenames.add(format("mise/config.%s.toml", miseEnv));
        allMiseConfigFilenames.add(format("mise.%s.toml", miseEnv));
        allMiseConfigFilenames.add(format(".mise/config.%s.toml", miseEnv));
        allMiseConfigFilenames.add(format(".mise.%s.toml", miseEnv));
        allMiseConfigFilenames.add(format(".config/mise/config.%s.local.toml", miseEnv));
        allMiseConfigFilenames.add(format("mise/config.%s.local.toml", miseEnv));
        allMiseConfigFilenames.add(format(".mise/config.%s.local.toml", miseEnv));
        allMiseConfigFilenames.add(format(".mise.%s.local.toml", miseEnv));

        return allMiseConfigFilenames;
    }

    /**
     * Ordering this hierarchy of reading the files isn't just the most idiomatic, it's also probably the best
     * for performance.
     */
    public static String recursivelyFindVersion(File directory, EventData eventData) throws Exception {
        if (!directory.canRead()) {
            throw new Exception("Tried to find a Node version file but giving up because it's not possible to read " +
                    directory.getPath());
        }

        String directoryPath = directory.getPath();

        Path nodeVersionFilePath = Paths.get(directoryPath, ".node-version");
        File nodeVersionFile = nodeVersionFilePath.toFile();
        if (nodeVersionFile.exists()) {
            String trimmedLine = readNvmrcFile(nodeVersionFile, nodeVersionFilePath);
            if (trimmedLine != null) {
                reportFoundVersion(NODE_VERSION, trimmedLine, eventData);
                return trimmedLine;
            }
        }

        Path nvmrcFilePath = Paths.get(directoryPath, ".nvmrc");
        File nvmrcFile = nvmrcFilePath.toFile();
        if (nvmrcFile.exists()) {
            String trimmedLine = readNvmrcFile(nvmrcFile, nvmrcFilePath);
            if (trimmedLine != null) {
                reportFoundVersion(NVMRC, trimmedLine, eventData);
                return trimmedLine;
            }
        }

        Path toolVersionsFilePath = Paths.get(directoryPath, TOOL_VERSIONS_FILENAME);
        File toolVersionsFile = toolVersionsFilePath.toFile();
        if (toolVersionsFile.exists()) {
            String trimmedLine = readToolVersionsFile(toolVersionsFile, toolVersionsFilePath);
            if (trimmedLine != null) {
                reportFoundVersion(TOOL_VERSIONS, trimmedLine, eventData);
                return trimmedLine;
            }
        }

        for (String miseConfigFilename: listMiseConfigFilenames()) {
            // We don't know if MISE_CONFIG_DIR can result in absolute or relative file paths, try to do our best
            String[] splitMiseConfigFilename = miseConfigFilename.split("/");
            Path potentiallyAbsoluteFilepath = Paths.get("", splitMiseConfigFilename);
            Path miseConfigFilePath = potentiallyAbsoluteFilepath.isAbsolute() ?
                    potentiallyAbsoluteFilepath : Paths.get(directoryPath, splitMiseConfigFilename);

            File miseConfigFile = miseConfigFilePath.toFile();
            if (miseConfigFile.exists()) {
                String trimmedVersion = readMiseConfigTomlFile(miseConfigFile, miseConfigFilePath);
                if (trimmedVersion != null) {
                    reportFoundVersion(MISE, trimmedVersion, eventData);
                    return trimmedVersion;
                }
            }
        }

        File parent = directory.getParentFile();
        if (isNull(parent) || directory.equals(parent)) {
            throw new Exception("Reach root-level without finding a suitable file");
        }

        return recursivelyFindVersion(parent, eventData);
    }

    private static String readNvmrcFile(File nvmrcFile, Path nvmrcFilePath) throws Exception {
        assertNodeVersionFileIsReadable(nvmrcFile);

        List<String> lines = Files.readAllLines(nvmrcFilePath);
        Optional<String> version = readNvmrcFileLines(lines);
        if (version.isPresent()) {
            logger.info("Found the version of Node in: " + nvmrcFilePath.normalize());
        }
        return version.orElse(null);
    }

    /**
     * We skip over a lot of comments. If there's no documentation in the POMs then we need it somewhere. Also, FNM,
     * NVS, and NVM have varying levels of comment acceptance, so we have to be the most forgiving.
     */
    @VisibleForTesting
    static Optional<String> readNvmrcFileLines(List<String> lines) {
        for (String line: lines) {
            if (!isNull(line)) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) {
                    continue;
                }

                if (trimmedLine.startsWith("#") || trimmedLine.startsWith("/") || trimmedLine.startsWith("!")) {
                    continue;
                }

                trimmedLine = trimmedLine.replaceFirst(
                        "(" + // we only want what's part of the comment, we assume everything at the start is the
                                // version
                        "\\s*" + // Okay, fine we also remove any whitespace too, this isn't part of the version
                        "[#!/]" + // these characters will probably not be part of the version, but they look like the
                                // start of a comment
                        ".*)", // everything else to the end of the line
                        "");

                return Optional.of(trimmedLine);
            }
        }
        return empty();
    }

    /**
     * If this gets any more complicated we'll add a reader, not sure how strict mise is with the spec, we want to be
     * at least as loose.
     */
    @VisibleForTesting
    static String readMiseConfigTomlFile(File miseTomlFile, Path miseTomlFilePath) throws Exception {
        assertNodeVersionFileIsReadable(miseTomlFile);

        List<String> lines = Files.readAllLines(miseTomlFilePath);
        for (String line: lines) {
            if (!isNull(line)) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) {
                    continue;
                }

                if (!trimmedLine.startsWith("node")) { // naturally skips over comments
                    continue;
                }

                logger.info("Found the version of Node in: " + miseTomlFilePath.normalize());

                if (trimmedLine.contains("[")) {
                    throw new Exception("mise file support is limited to a single version");
                }

                return trimmedLine
                        .replaceAll("node(js)?\\s*=\\s*", "")
                        .replaceAll("\"", "") // destringify the version -- there's no " in Node versions
                        .replaceAll("#.*$", "") // remove comments -- there's no '#' in Node versions
                        .trim();
            }
        }
        return null;
    }

    private static String readToolVersionsFile(File toolVersionsFile, Path toolVersionsFilePath) throws Exception {
        assertNodeVersionFileIsReadable(toolVersionsFile);

        List<String> lines = Files.readAllLines(toolVersionsFilePath);
        for (String line: lines) {
            if (!isNull(line)) {
                String trimmedLine = line.trim();

                if (trimmedLine.isEmpty()) {
                    continue;
                }

                if (!trimmedLine.startsWith("node")) {
                    continue;
                }

                logger.info("Found the version of Node in: " + toolVersionsFilePath.normalize());
                return trimmedLine.replaceAll("node(js)?\\s*", "");
            }
        }
        return null;
    }

    private static void assertNodeVersionFileIsReadable(File file) throws Exception {
        if (!file.canRead()) {
            throw new Exception("Tried to read the node version from the file, but giving up because it's not possible to read" + file.getPath());
        }
    }

    private static void reportFoundVersion(String location, String nodeVersion, EventData eventData) {
        if (!validateVersion(nodeVersion)) {
            return; // this is going to fail
        }

        nodeVersion = getDownloadableVersion(nodeVersion);

        incrementCount(
                "runtime.version",
                eventData.artifactId,
                eventData.forkVersion,
                nodeVersion,
                new HashMap<String, String>() {{
                    put("version-location", location);
                }});
    }

    @VisibleForTesting
    static class EventData {
        private final String artifactId, forkVersion;

        EventData(
                String artifactId,
                String forkVersion) {
            this.artifactId = artifactId;
            this.forkVersion = forkVersion;
        }
    }

    public interface NodeVersionLocations {
        String NVMRC = "nvmrc";
        String NODE_VERSION = "node-version";
        String MAVEN = "maven";
        String MISE = "mise";
        String TOOL_VERSIONS = "tool-versions";
        String UNKNOWN = "unknown";
    }
}
