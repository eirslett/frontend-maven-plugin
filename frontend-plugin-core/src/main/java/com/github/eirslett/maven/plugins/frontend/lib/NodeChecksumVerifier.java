package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.function.Supplier;

class NodeChecksumVerifier {
    private final String sha256txt;

    public NodeChecksumVerifier(Supplier<String> sha256Supplier) {
        sha256txt = sha256Supplier.get();
    }

    boolean isChecksumValid(File archive) {
        byte[] requiredChecksum = readRequiredChecksumOf(archive, sha256txt);
        byte[] actualChecksum = calculateChecksumOf(archive);

        return Arrays.equals(requiredChecksum, actualChecksum);
    }

    private static byte[] calculateChecksumOf(File file) {
        MessageDigest sha256 = newSha256Digest();

        try (FileInputStream fis = new FileInputStream(file)) {
            BufferedInputStream bis = new BufferedInputStream(fis);
            DigestInputStream dis = new DigestInputStream(bis, sha256);

            while (dis.read() != -1) {
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sha256.digest();
    }

    private static byte[] readRequiredChecksumOf(File file, String sha256txt) {
        String checksumHex = Arrays.stream(sha256txt.split("\\R"))
                .filter(line -> line.contains(file.getName()))
                .map(line -> line.split(" {2}")[0])
                .findFirst()
                .get();

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
