package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Resolves the Node.js version to install from an explicit configuration value,
 * a configured version file, or a default {@code .node-version} file.
 */
public final class NodeVersionHelper {

    public static final String NODE_VERSION_FILE_NAME = ".node-version";

    private NodeVersionHelper() {
    }

    /**
     * Resolve the Node.js version using the following precedence:
     * <ol>
     *   <li>{@code nodeVersion} when set to a non-blank value</li>
     *   <li>contents of {@code nodeVersionFile} when configured</li>
     *   <li>contents of {@code .node-version} in {@code workingDirectory} when present</li>
     * </ol>
     *
     * @param nodeVersion
     *         explicit Node.js version from plugin configuration, or {@code null}/blank when unset
     * @param nodeVersionFile
     *         optional path to a version file; when set, the file must exist
     * @param workingDirectory
     *         directory used to look up the default {@code .node-version} file
     * @return the resolved Node.js version (with a leading {@code v} when read from a file)
     * @throws InstallationException
     *         if no version can be determined, or a configured/default file cannot be read
     */
    public static String resolveNodeVersion(String nodeVersion, File nodeVersionFile, File workingDirectory)
            throws InstallationException {
        if (isNotBlank(nodeVersion)) {
            return nodeVersion.trim();
        }

        if (nodeVersionFile != null) {
            if (!nodeVersionFile.isFile()) {
                throw new InstallationException(
                        "The configured nodeVersionFile does not exist or is not a file: "
                                + nodeVersionFile.getAbsolutePath());
            }
            return readNodeVersionFromFile(nodeVersionFile);
        }

        if (workingDirectory == null) {
            throw new InstallationException(missingVersionMessage(null));
        }

        File defaultNodeVersionFile = new File(workingDirectory, NODE_VERSION_FILE_NAME);
        if (defaultNodeVersionFile.isFile()) {
            return readNodeVersionFromFile(defaultNodeVersionFile);
        }

        throw new InstallationException(missingVersionMessage(workingDirectory));
    }

    /**
     * Read a Node.js version from a version file.
     * <p>
     * The first non-empty, non-comment line is used. A leading {@code v} is added when missing,
     * so values like {@code 24.12.0} become {@code v24.12.0} for download URL compatibility.
     *
     * @param file
     *         the version file to read
     * @return the normalized Node.js version
     * @throws InstallationException
     *         if the file cannot be read or does not contain a usable version
     */
    public static String readNodeVersionFromFile(File file) throws InstallationException {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                return normalizeNodeVersion(trimmed);
            }
        } catch (IOException e) {
            throw new InstallationException(
                    "Failed to read Node.js version from " + file.getAbsolutePath(), e);
        }

        throw new InstallationException(
                "Node.js version file is empty or contains no usable version: " + file.getAbsolutePath());
    }

    /**
     * Ensure a Node.js version string starts with {@code v}.
     *
     * @param version
     *         raw version string (already trimmed)
     * @return version with a leading {@code v}
     */
    static String normalizeNodeVersion(String version) {
        if (version.startsWith("v") || version.startsWith("V")) {
            return "v" + version.substring(1);
        }
        return "v" + version;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String missingVersionMessage(File workingDirectory) {
        String workingDirectoryHint = workingDirectory == null
                ? ""
                : " in the working directory (" + workingDirectory.getAbsolutePath() + ")";
        return "Node.js version could not be determined. Set <nodeVersion>, configure <nodeVersionFile>, "
                + "or provide a " + NODE_VERSION_FILE_NAME + " file" + workingDirectoryHint + ".";
    }
}
