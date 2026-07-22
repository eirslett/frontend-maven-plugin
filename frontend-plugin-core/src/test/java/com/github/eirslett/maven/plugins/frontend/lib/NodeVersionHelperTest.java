package com.github.eirslett.maven.plugins.frontend.lib;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeVersionHelperTest {

    @TempDir
    Path tempDir;

    @Test
    public void resolveNodeVersion_prefersExplicitNodeVersion() throws Exception {
        File versionFile = writeFile(tempDir.resolve("custom-version"), "v18.0.0");
        writeFile(tempDir.resolve(NodeVersionHelper.NODE_VERSION_FILE_NAME), "v20.0.0");

        String resolved = NodeVersionHelper.resolveNodeVersion(
                "v24.12.0", versionFile, tempDir.toFile());

        assertEquals("v24.12.0", resolved);
    }

    @Test
    public void resolveNodeVersion_usesNodeVersionFileWhenNodeVersionMissing() throws Exception {
        File versionFile = writeFile(tempDir.resolve("versions/node.txt"), "22.11.0");

        String resolved = NodeVersionHelper.resolveNodeVersion(
                null, versionFile, tempDir.toFile());

        assertEquals("v22.11.0", resolved);
    }

    @Test
    public void resolveNodeVersion_prefersConfiguredFileOverDefaultFile() throws Exception {
        File versionFile = writeFile(tempDir.resolve("config/node.txt"), "v18.20.0");
        writeFile(tempDir.resolve(NodeVersionHelper.NODE_VERSION_FILE_NAME), "v20.0.0");

        String resolved = NodeVersionHelper.resolveNodeVersion(
                null, versionFile, tempDir.toFile());

        assertEquals("v18.20.0", resolved);
    }

    @Test
    public void resolveNodeVersion_treatsBlankNodeVersionAsUnset() throws Exception {
        writeFile(tempDir.resolve(NodeVersionHelper.NODE_VERSION_FILE_NAME), "v24.12.0");

        String resolved = NodeVersionHelper.resolveNodeVersion(
                "   ", null, tempDir.toFile());

        assertEquals("v24.12.0", resolved);
    }

    @Test
    public void resolveNodeVersion_fallsBackToDefaultNodeVersionFile() throws Exception {
        writeFile(tempDir.resolve(NodeVersionHelper.NODE_VERSION_FILE_NAME), "24.12.0");

        String resolved = NodeVersionHelper.resolveNodeVersion(
                null, null, tempDir.toFile());

        assertEquals("v24.12.0", resolved);
    }

    @Test
    public void resolveNodeVersion_failsWhenConfiguredFileMissing() {
        File missing = tempDir.resolve("missing-version").toFile();

        InstallationException exception = assertThrows(
                InstallationException.class,
                () -> NodeVersionHelper.resolveNodeVersion(null, missing, tempDir.toFile()));

        assertTrue(exception.getMessage().contains("does not exist"));
        assertTrue(exception.getMessage().contains(missing.getAbsolutePath()));
    }

    @Test
    public void resolveNodeVersion_failsWhenNothingConfigured() {
        InstallationException exception = assertThrows(
                InstallationException.class,
                () -> NodeVersionHelper.resolveNodeVersion(null, null, tempDir.toFile()));

        assertTrue(exception.getMessage().contains("Node.js version could not be determined"));
        assertTrue(exception.getMessage().contains(NodeVersionHelper.NODE_VERSION_FILE_NAME));
    }

    @Test
    public void readNodeVersionFromFile_skipsCommentsAndBlankLines() throws Exception {
        File versionFile = writeFile(
                tempDir.resolve(NodeVersionHelper.NODE_VERSION_FILE_NAME),
                "# chosen by the team\n\n  v24.12.0  \n");

        assertEquals("v24.12.0", NodeVersionHelper.readNodeVersionFromFile(versionFile));
    }

    @Test
    public void readNodeVersionFromFile_normalizesUppercaseVPrefix() throws Exception {
        File versionFile = writeFile(tempDir.resolve("version"), "V24.12.0");

        assertEquals("v24.12.0", NodeVersionHelper.readNodeVersionFromFile(versionFile));
    }

    @Test
    public void readNodeVersionFromFile_failsWhenEmpty() throws Exception {
        File versionFile = writeFile(tempDir.resolve("empty"), "# only a comment\n\n");

        InstallationException exception = assertThrows(
                InstallationException.class,
                () -> NodeVersionHelper.readNodeVersionFromFile(versionFile));

        assertTrue(exception.getMessage().contains("empty or contains no usable version"));
    }

    @Test
    public void normalizeNodeVersion_addsMissingPrefix() {
        assertEquals("v24.12.0", NodeVersionHelper.normalizeNodeVersion("24.12.0"));
        assertEquals("v24.12.0", NodeVersionHelper.normalizeNodeVersion("v24.12.0"));
    }

    private static File writeFile(Path path, String contents) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, contents.getBytes(StandardCharsets.UTF_8));
        return path.toFile();
    }
}
