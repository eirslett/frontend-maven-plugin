package com.github.eirslett.maven.plugins.frontend.lib;

import org.junit.jupiter.api.Test;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlatformTest {

    private static final String NODE_VERSION_8 = "v8.17.2";
    private static final String NODE_VERSION_15 = "v15.14.0";
    private static final String NODE_VERSION_16 = "v16.1.0";

    @Test
    public void detect_win_doesntLookForAlpine() {
        // Throw if called
        Supplier<Boolean> checkForAlpine = () -> {
            throw new RuntimeException("Shouldn't be called");
        };

        Platform platform = Platform.guess(OS.Windows, Architecture.x86, () -> false);
        assertEquals("win-x86", platform.getNodeClassifier(NODE_VERSION_15));
    }

    @Test
    public void detect_arm_mac_download_x64_binary_node15() {
        Platform platform = Platform.guess(OS.Mac, Architecture.arm64, () -> false);
        assertEquals("darwin-x64", platform.getNodeClassifier(NODE_VERSION_15));
    }

    @Test
    public void detect_arm_mac_download_x64_binary_node16() {
        Platform platform = Platform.guess(OS.Mac, Architecture.arm64, () -> false);
        assertEquals("darwin-arm64", platform.getNodeClassifier(NODE_VERSION_16));
    }

    @Test
    public void detect_linux_notAlpine() throws Exception {
        Platform platform = Platform.guess(OS.Linux, Architecture.x86, () -> false);
        assertEquals("linux-x86", platform.getNodeClassifier(NODE_VERSION_15));
        assertEquals("https://nodejs.org/dist/", platform.getNodeDownloadRoot());
    }

    @Test
    public void detect_linux_alpine() throws Exception {
        Platform platform = Platform.guess(OS.Linux, Architecture.x86, () -> true);
        assertEquals("linux-x86-musl", platform.getNodeClassifier(NODE_VERSION_15));
        assertEquals("https://unofficial-builds.nodejs.org/download/release/",
                platform.getNodeDownloadRoot());
    }

    @Test
    public void detect_aix_ppc64() {
        Platform platform = Platform.guess(OS.AIX, Architecture.ppc64, () -> false);
        assertEquals("aix-ppc64", platform.getNodeClassifier(NODE_VERSION_15));
    }

    @Test
    public void getNodeMajorVersion() {
        assertEquals(Integer.valueOf(8), Platform.getNodeMajorVersion(NODE_VERSION_8));
        assertEquals(Integer.valueOf(15), Platform.getNodeMajorVersion(NODE_VERSION_15));
        assertEquals(Integer.valueOf(16), Platform.getNodeMajorVersion(NODE_VERSION_16));
    }

}
