package com.github.eirslett.maven.plugins.frontend.lib;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class DefaultArchiveExtractorTest {

    private final String BAD_TAR = "src/test/resources/bad.tgz";
    private final String GOOD_TAR = "src/test/resources/good.tgz";

    @TempDir
    public File temp;

    private ArchiveExtractor extractor;

    @BeforeEach
    public void setup() {
        extractor = new DefaultArchiveExtractor();
    }

    @Test
    public void extractGoodTarFile() throws Exception {
        extractor.extract(GOOD_TAR, temp.getPath());
    }

    @Test
    public void extractGoodTarFileSymlink() throws Exception {
        File destination = new File(temp.getPath() + "/destination");
        destination.mkdir();
        Path link = createSymlinkOrSkipTest(temp.toPath().resolve("link"), destination.toPath());
        extractor.extract(GOOD_TAR, link.toString());
    }

    @Test
    public void extractBadTarFile() {
        Assertions.assertThrows(ArchiveExtractionException.class, () ->
                extractor.extract(BAD_TAR, temp.getPath()));
    }

    @Test
    public void extractBadTarFileSymlink() {
        File destination = new File(temp + "/destination");
        destination.mkdir();
        Path link = createSymlinkOrSkipTest(destination.toPath().resolve("link"), destination.toPath());
        Assertions.assertThrows(ArchiveExtractionException.class, () -> extractor.extract(BAD_TAR, link.toString()));
    }

    private Path createSymlinkOrSkipTest(Path link, Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symlinks not supported");
            return null;
        }
    }
}
