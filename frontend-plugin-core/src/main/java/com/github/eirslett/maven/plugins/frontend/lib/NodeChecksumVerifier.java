package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

class NodeChecksumVerifier {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String sha256txt;

    NodeChecksumVerifier(String sha256Txt) {
        this.sha256txt = sha256Txt;
    }

    boolean isChecksumValid(File archive) {
        byte[] requiredChecksum = readRequiredChecksumOf(archive, sha256txt);
        byte[] actualChecksum = calculateChecksumOf(archive);

        return Arrays.equals(requiredChecksum, actualChecksum);
    }

    private byte[] calculateChecksumOf(File file) {
        MessageDigest sha256 = newSha256Digest();

        try (FileInputStream fis = new FileInputStream(file)) {
            BufferedInputStream bis = new BufferedInputStream(fis);
            DigestInputStream dis = new DigestInputStream(bis, sha256);

            while (dis.read() != -1) {
                // read to end
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sha256.digest();
    }

    private byte[] readRequiredChecksumOf(File file, String sha256txt) {
        logger.debug("Searching for required checksum in file.");
        logger.debug("Filename: {}", file.getName());
        logger.debug("Checksum file:\n{}", sha256txt);

        String checksumHex = Arrays.stream(sha256txt.split("\\R"))
                .map(line -> line.split(" {2}"))
                .filter(line -> line[1].replaceAll("-v", "-").contains(file.getName()))
                .map(line -> line[0])
                .findFirst()
                .orElseThrow(() -> new RuntimeException(String.format("Failed to find checksum for filename %s\n", file.getName()) +
                        "This is likely an issue with the frontend-maven-plugin itself. " +
                        "Please report an issue at https://github.com/eirslett/frontend-maven-plugin"));

        try {
            return Hex.decodeHex(checksumHex);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
