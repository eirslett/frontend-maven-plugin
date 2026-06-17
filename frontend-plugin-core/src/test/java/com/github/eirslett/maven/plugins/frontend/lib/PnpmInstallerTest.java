package com.github.eirslett.maven.plugins.frontend.lib;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PnpmInstallerTest {

    @TempDir
    public File temp;

    @Test
    public void replacesBrokenPnpmSymlink() throws Exception {
        File installDirectory = new File(temp, "install");
        Path nodeDirectory = installDirectory.toPath().resolve("node");
        Path pnpmBinDirectory = nodeDirectory.resolve("node_modules/pnpm/bin");
        Files.createDirectories(pnpmBinDirectory);
        Files.writeString(nodeDirectory.resolve("node_modules/pnpm/package.json"), "{\"version\":\"10.0.0\"}");
        Files.writeString(pnpmBinDirectory.resolve("pnpm.cjs"), "");
        createSymlinkOrSkipTest(nodeDirectory.resolve("pnpm"), temp.toPath().resolve("missing-container-path/pnpm"));

        PnpmInstaller installer = new FrontendPluginFactory(temp, installDirectory)
            .getPnpmInstaller(new ProxyConfig(Collections.emptyList()))
            .setPnpmVersion("10.0.0");

        assertDoesNotThrow(installer::install);

        assertEquals(pnpmBinDirectory.resolve("pnpm.cjs"), Files.readSymbolicLink(nodeDirectory.resolve("pnpm")));
    }

    private Path createSymlinkOrSkipTest(Path link, Path target) {
        try {
            return Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            assumeTrue(false, "symlinks not supported");
            return null;
        }
    }
}
