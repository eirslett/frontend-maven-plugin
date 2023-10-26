package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NodeChecksumVerifierTest {
    @Test
    void isChecksumValid_withValidChecksum_returnsTrue() throws IOException, URISyntaxException {
        String shasumsTxt = IOUtils.toString(getResource("SHASUMS256.txt"), StandardCharsets.UTF_8);
        File archiveToCheck = new File(getResource("good/node-18.18.2-headers.tar.xz").toURI());

        NodeChecksumVerifier verifier = new NodeChecksumVerifier(shasumsTxt);
        boolean isValid = verifier.isChecksumValid(archiveToCheck);

        assertThat(isValid, is(true));
    }

    @Test
    void isChecksumValid_withInvalidChecksum_returnsFalse() throws IOException, URISyntaxException {
        String shasumsTxt = IOUtils.toString(getResource("SHASUMS256.txt"), StandardCharsets.UTF_8);
        File archiveToCheck = new File(getResource("bad/node-18.18.2-headers.tar.xz").toURI());

        NodeChecksumVerifier verifier = new NodeChecksumVerifier(shasumsTxt);
        boolean isValid = verifier.isChecksumValid(archiveToCheck);

        assertThat(isValid, is(false));
    }

    private URL getResource(String path) {
        return getClass().getClassLoader().getResource(String.format("checksum/%s", path));
    }
}
