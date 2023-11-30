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
    private final String BAD_ZIP = "src/test/resources/bad.zip"; // sample zip with zip slip vulnerability
    private final String GOOD_TAR = "src/test/resources/good.tgz";
    private final String GOOD_ZIP = "src/test/resources/good.zip";

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
    public void extractGoodZipFile() throws Exception {
        assertGoodZipExtractedTo(temp);
    }

    @Test
    public void extractGoodZipFileWithRelTarget() throws Exception {
        assertGoodZipExtractedTo(createRelPath(temp));
    }

    @Test
    public void extractBadZipFile() {
        assertBadZipThrowsException(temp);
    }

    @Test
    public void extractBadZipFileWithRelTarget() throws Exception {
        assertBadZipThrowsException(createRelPath(temp));
    }

    @Test
    public void extractBadTarFileSymlink() {
        File destination = new File(temp + "/destination");
        destination.mkdir();
        Path link = createSymlinkOrSkipTest(destination.toPath().resolve("link"), destination.toPath());
        Assertions.assertThrows(ArchiveExtractionException.class, () -> extractor.extract(BAD_TAR, link.toString()));
    }

    private void assertBadZipThrowsException(File targetDir) {
        Assertions.assertThrows(RuntimeException.class, () ->
                extractor.extract(BAD_ZIP, targetDir.getPath()));
    }

    private void assertGoodZipExtractedTo(File targetDir) throws Exception {
        extractor.extract(GOOD_ZIP, targetDir.getPath());
        String nameOfFileInZip = "zip";
        Assertions.assertTrue(new File(temp, nameOfFileInZip).isFile(), "zip content not found in target directory");
    }

    private static File createRelPath(File orig) throws IOException {
        orig = orig.getCanonicalFile();
        String dirName = orig.getName();
        File result = new File(orig, "../" + dirName);
        Assertions.assertNotEquals(orig, result); // ensure result is different from input
        Assertions.assertEquals(orig, result.getCanonicalFile()); // ensure result points to same dir
        return result;
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
