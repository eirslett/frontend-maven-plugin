package com.github.eirslett.maven.plugins.frontend.lib;

import com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.NodeVersionComparator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.UNUSUAL_VALID_VERSIONS;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.VALID_VERSION_PATTERN;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.getDownloadableVersion;
import static com.github.eirslett.maven.plugins.frontend.lib.NodeVersionHelper.validateVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeVersionHelperTest {

    @Test
    public void testUnusualPatterns_shouldNotMatchThePattern_toKeepTheListSmall() {
        UNUSUAL_VALID_VERSIONS.forEach(version -> {
            assertFalse(VALID_VERSION_PATTERN.matcher(version).find());
        });
    }

    @Test
    public void testUnusualPreviousVersions_shouldBeTreatedAsValid() {
        UNUSUAL_VALID_VERSIONS.forEach(version -> {
            assertTrue(validateVersion(version));
        });
    }

    @Test
    public void testVersionsMissingV_shouldBeFixed() {
        assertEquals("v1.0.0", getDownloadableVersion("1.0.0"));
    }

    @Test
    public void testInvalidCase_shouldBeFixed() {
        assertEquals("v1.0.0", getDownloadableVersion("V1.0.0"));
    }

    @Test
    public void testLooselyDefinedMajorVersions_shouldBeValid() {
        assertTrue(validateVersion("12"));
    }

    @Disabled("We need to figure out a better way than blocking on an HTTP request near the start")
    @Test
    public void testGetDownloadableVersion_shouldGiveUsTheLatestDownloadableVersion_forAGivenLooselyDefinedMajorVersion() {
        // Using Node 12 since there shouldn't be anymore releases
        assertEquals("v12.22.12", getDownloadableVersion("12"));
    }

    @Test
    public void testNodeVersionComparator_shouldCompareByNumbers() {
        assertEquals(-1, new NodeVersionComparator().compare("v1.1.9", "v1.1.10"));
    }

    @Test
    public void testNodeVersionComparator_shouldHandleEqualVersions() {
        assertEquals(0, new NodeVersionComparator().compare("v1.1.1", "v1.1.1"));
    }
}
