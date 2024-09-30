package com.github.eirslett.maven.plugins.frontend.lib;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;

public class NodeVersionDetector {

    private static final String TOOL_VERSIONS_FILENAME = ".tool-versions";

    public static String getNodeVersion(File workingDir, String providedNodeVersion, String genericNodeVersionFile) throws Exception {
        Logger logger = getLogger(NodeVersionDetector.class);

        if (!isNull(providedNodeVersion) && !providedNodeVersion.trim().isEmpty()) {
            logger.debug("Looks like a node version was set so using that: " + providedNodeVersion);
            return providedNodeVersion;
        }

        if (!isNull(genericNodeVersionFile) && !genericNodeVersionFile.trim().isEmpty()) {
            File genericNodeVersionFileFile = new File(genericNodeVersionFile);
            if (!genericNodeVersionFileFile.exists()) {
                throw new Exception("The Node version file doesn't seem to exist: " + genericNodeVersionFileFile);
            }

            if (genericNodeVersionFile.endsWith(".toml") && genericNodeVersionFile.contains("mise")) {
                return readMiseConfigTomlFile(genericNodeVersionFileFile, genericNodeVersionFileFile.toPath(), logger);
            } else if (genericNodeVersionFile.endsWith(TOOL_VERSIONS_FILENAME)) {
                return readToolVersionsFile(genericNodeVersionFileFile, genericNodeVersionFileFile.toPath(), logger);
            } else {
                return readNvmrcFile(genericNodeVersionFileFile, genericNodeVersionFileFile.toPath(), logger);
            }
        }

        try {
            return recursivelyFindVersion(workingDir);
        } catch (Throwable throwable) {
            logger.debug("Going to use the configuration node version, failed to find a file with the version because",
                    throwable);
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
    public static String recursivelyFindVersion(File directory) throws Exception {
        Logger logger = getLogger(NodeVersionDetector.class);

        if (!directory.canRead()) {
            throw new Exception("Tried to find a Node version file but giving up because it's not possible to read " +
                    directory.getPath());
        }

        String directoryPath = directory.getPath();

        Path nodeVersionFilePath = Paths.get(directoryPath, ".node-version");
        File nodeVersionFile = nodeVersionFilePath.toFile();
        if (nodeVersionFile.exists()) {
            String trimmedLine = readNvmrcFile(nodeVersionFile, nodeVersionFilePath, logger);
            if (trimmedLine != null) return trimmedLine;
        }

        Path nvmrcFilePath = Paths.get(directoryPath, ".nvmrc");
        File nvmrcFile = nvmrcFilePath.toFile();
        if (nvmrcFile.exists()) {
            String trimmedLine = readNvmrcFile(nvmrcFile, nvmrcFilePath, logger);
            if (trimmedLine != null) return trimmedLine;
        }

        Path toolVersionsFilePath = Paths.get(directoryPath, TOOL_VERSIONS_FILENAME);
        File toolVersionsFile = toolVersionsFilePath.toFile();
        if (toolVersionsFile.exists()) {
            String trimmedLine = readToolVersionsFile(toolVersionsFile, toolVersionsFilePath, logger);
            if (trimmedLine != null) return trimmedLine;
        }

        for (String miseConfigFilename: listMiseConfigFilenames()) {
            // We don't know if MISE_CONFIG_DIR can result in absolute or relative file paths, try to do our best
            String[] splitMiseConfigFilename = miseConfigFilename.split("/");
            Path potentiallyAbsoluteFilepath = Paths.get("", splitMiseConfigFilename);
            Path miseConfigFilePath = potentiallyAbsoluteFilepath.isAbsolute() ?
                    potentiallyAbsoluteFilepath : Paths.get(directoryPath, splitMiseConfigFilename);

            File miseConfigFile = miseConfigFilePath.toFile();
            if (miseConfigFile.exists()) {
                String trimmedVersion = readMiseConfigTomlFile(miseConfigFile, miseConfigFilePath, logger);
                if (trimmedVersion != null) return trimmedVersion;
            }
        }

        File parent = directory.getParentFile();
        if (isNull(parent) || directory.equals(parent)) {
            throw new Exception("Reach root-level without finding a suitable file");
        }

        return recursivelyFindVersion(parent);
    }

    private static String readNvmrcFile(File nvmrcFile, Path nvmrcFilePath, Logger logger) throws Exception {
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
    static String readMiseConfigTomlFile(File miseTomlFile, Path miseTomlFilePath, Logger logger) throws Exception {
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

    private static String readToolVersionsFile(File toolVersionsFile, Path toolVersionsFilePath, Logger logger) throws Exception {
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
}
