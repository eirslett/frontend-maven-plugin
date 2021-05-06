package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Platform.class, OS.class, Architecture.class, File.class})
public class PlatformTest {

    private static final String NODE_VERSION_8 = "v8.17.2";
    private static final String NODE_VERSION_15 = "v15.14.0";
    private static final String NODE_VERSION_16 = "v16.1.0";

    @Test
    public void detect_win_doesntLookForAlpine() {
        mockStatic(OS.class);
        mockStatic(Architecture.class);

        when(OS.guess()).thenReturn(OS.Windows);
        when(Architecture.guess()).thenReturn(Architecture.x86);

        Platform platform = Platform.guess();
        assertEquals("win-x86", platform.getNodeClassifier(NODE_VERSION_15));

        verifyNoMoreInteractions(File.class); // doesn't look for a file path
    }

    @Test
    public void detect_arm_mac_download_x64_binary_node15() {
        mockStatic(OS.class);
        mockStatic(Architecture.class);

        when(OS.guess()).thenReturn(OS.Mac);
        when(Architecture.guess()).thenReturn(Architecture.arm64);

        Platform platform = Platform.guess();
        assertEquals("darwin-x64", platform.getNodeClassifier(NODE_VERSION_15));
    }

    @Test
    public void detect_arm_mac_download_x64_binary_node16() {
        mockStatic(OS.class);
        mockStatic(Architecture.class);

        when(OS.guess()).thenReturn(OS.Mac);
        when(Architecture.guess()).thenReturn(Architecture.arm64);

        Platform platform = Platform.guess();
        assertEquals("darwin-arm64", platform.getNodeClassifier(NODE_VERSION_16));
    }

    @Test
    public void detect_linux_notAlpine() throws Exception {
        mockStatic(OS.class);
        mockStatic(Architecture.class);

        when(OS.guess()).thenReturn(OS.Linux);
        when(Architecture.guess()).thenReturn(Architecture.x86);

        File alpineRelease = mock(File.class);
        whenNew(File.class)
                .withArguments("/etc/alpine-release").thenReturn(alpineRelease);

        when(alpineRelease.exists()).thenReturn(false);

        Platform platform = Platform.guess();
        assertEquals("linux-x86", platform.getNodeClassifier(NODE_VERSION_15));
        assertEquals("https://nodejs.org/dist/", platform.getNodeDownloadRoot());
    }

    @Test
    public void detect_linux_alpine() throws Exception {
        mockStatic(OS.class);
        mockStatic(Architecture.class);

        when(OS.guess()).thenReturn(OS.Linux);
        when(Architecture.guess()).thenReturn(Architecture.x86);

        File alpineRelease = mock(File.class);
        whenNew(File.class).withArguments("/etc/alpine-release")
            .thenReturn(alpineRelease);

        when(alpineRelease.exists()).thenReturn(true);

        Platform platform = Platform.guess();
        assertEquals("linux-x86-musl", platform.getNodeClassifier(NODE_VERSION_15));
        assertEquals("https://unofficial-builds.nodejs.org/download/release/",
                platform.getNodeDownloadRoot());
    }

    @Test
    public void detect_aix_ppc64() {
        mockStatic(OS.class);
        mockStatic(Architecture.class);

        when(OS.guess()).thenReturn(OS.AIX);
        when(Architecture.guess()).thenReturn(Architecture.ppc64);

        Platform platform = Platform.guess();
        assertEquals("aix-ppc64", platform.getNodeClassifier(NODE_VERSION_15));
    }

    @Test
    public void getNodeMajorVersion() {
        assertEquals(Integer.valueOf(8), Platform.getNodeMajorVersion(NODE_VERSION_8));
        assertEquals(Integer.valueOf(15), Platform.getNodeMajorVersion(NODE_VERSION_15));
        assertEquals(Integer.valueOf(16), Platform.getNodeMajorVersion(NODE_VERSION_16));
    }

}
