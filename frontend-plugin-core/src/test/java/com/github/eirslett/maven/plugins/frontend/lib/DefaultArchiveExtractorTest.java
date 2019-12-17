package com.github.eirslett.maven.plugins.frontend.lib;

import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class DefaultArchiveExtractorTest {

    private final String BAD_TAR = "src/test/resources/bad.tgz";
    private final String GOOD_TAR = "src/test/resources/good.tgz";

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private ArchiveExtractor extractor;

    @Before
    public void setup() {
        extractor = new DefaultArchiveExtractor();
    }

    @Test
    public void extractGoodTarFile() throws Exception {
        File destination = temp.newFolder("destination");
        extractor.extract(GOOD_TAR, destination.getPath());
    }

    @Test
    public void extractGoodTarFileSymlink() throws Exception {
        File destination = temp.newFolder("destination");
        Path link = createSymlinkOrSkipTest(temp.getRoot().toPath().resolve("link"), destination.toPath());
        extractor.extract(GOOD_TAR, link.toString());
    }

    @Test(expected = ArchiveExtractionException.class)
    public void extractBadTarFile() throws Exception {
        File destination = temp.newFolder("destination");
        extractor.extract(BAD_TAR, destination.getPath());
    }

    @Test(expected = ArchiveExtractionException.class)
    public void extractBadTarFileSymlink() throws Exception {
        File destination = temp.newFolder("destination");
        Path link = createSymlinkOrSkipTest(temp.getRoot().toPath().resolve("link"), destination.toPath());
        extractor.extract(BAD_TAR, link.toString());
    }

    private Path createSymlinkOrSkipTest(Path link, Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue("symlinks not supported", false);
            return null;
        }
    }
}
