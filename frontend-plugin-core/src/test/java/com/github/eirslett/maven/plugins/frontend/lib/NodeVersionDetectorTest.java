package com.github.eirslett.maven.plugins.frontend.lib;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.readMiseConfigTomlFile;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.readNvmrcFileLines;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionDetector.recursivelyFindVersion;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeVersionDetectorTest {

    @Test
    public void testNvmrcFileParsing_shouldWorkWithACommentWithWhiteSpaceOnTheSameLineAsTheVersion() {
        assertEquals("v1.0.0", readNvmrcFileLines(singletonList("v1.0.0\t //\t comment")).get());
    }

    @Test
    public void testNvmrcFileParsing_shouldIgnoreCommentOnlyLines() {
        assertEquals("v1.0.0", readNvmrcFileLines(asList(
                "#comment",
                " ! comment",
                "\t/\tcomment",
                "v1.0.0",
                "#comment",
                " ! comment",
                "\t/\tcomment"
        )).get());
    }

    @Test
    public void testNvmrcFileParsing_shouldIgnoreEmptyLines() {
        assertEquals("v1.0.0", readNvmrcFileLines(asList(
                "\t",
                "\t \r",
                "",
                "v1.0.0",
                "\t",
                "\t \r",
                ""
        )).get());
    }

    @Test
    public void testMiseConfigFileParsing_shouldNotAllowVersionArrays() throws URISyntaxException {
        URL miseConfigFileUrl =
                Thread.currentThread().getContextClassLoader().getResource("miseConfig-nodeVersionArray.toml");
        Path miseConfigFilePath = Paths.get(miseConfigFileUrl.toURI());
        assertTrue(Files.exists(miseConfigFilePath)); // required for a valid test

        assertThrows(Exception.class,
                () -> readMiseConfigTomlFile(miseConfigFilePath.toFile(), miseConfigFilePath));
    }

    @Test
    public void testMiseConfigFileParsing_shouldReadValidFiles() throws Exception {
        URL miseConfigFileUrl =
                Thread.currentThread().getContextClassLoader().getResource("miseConfig-difficultButParseable.toml");
        Path miseConfigFilePath = Paths.get(miseConfigFileUrl.toURI());
        assertTrue(Files.exists(miseConfigFilePath)); // required for a valid test

        assertEquals("20.0.0", readMiseConfigTomlFile(miseConfigFilePath.toFile(), miseConfigFilePath));
    }

    @Test
    public void testAbsoluteMiseConfigFilePath(
            @TempDir File tempMiseConfigDir,
            @TempDir File tempUnrelatedDir
    ) throws Exception {
        // setup
        String expectedVersion = "9.8.7";
        String miseProfile = "testabsolute";

        String tempMiseConfigDirAbsolutePath = tempMiseConfigDir.getAbsolutePath();
        String tempMiseConfigFilename = format("mise.%s.toml", miseProfile);
        Path tempMiseConfigFilePath = Paths.get(tempMiseConfigDirAbsolutePath, tempMiseConfigFilename);
        Path tempMiseConfigFile = Files.createFile(tempMiseConfigFilePath);

        // given
        withEnvironmentVariable("MISE_CONFIG_DIR", tempMiseConfigDirAbsolutePath)
                .and("MISE_ENV", miseProfile)
                .execute(() -> {
                    String miseConfigFileContents = format("node = \"%s\"", expectedVersion);
                    Files.write(tempMiseConfigFile, singletonList(miseConfigFileContents), defaultCharset(), WRITE);

                    // when
                    String readVersion = recursivelyFindVersion(tempUnrelatedDir, new NodeVersionDetector.EventData("", ""));

                    // then
                    assertEquals(expectedVersion, readVersion, "versions didn't match");
                });
    }
}
