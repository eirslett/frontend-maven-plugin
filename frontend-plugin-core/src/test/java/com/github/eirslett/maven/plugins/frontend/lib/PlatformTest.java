package com.github.eirslett.maven.plugins.frontend.lib;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PlatformTest {

    private static final String NODE_VERSION_8 = "v8.17.2";
    private static final String NODE_VERSION_15 = "v15.14.0";
    private static final String NODE_VERSION_16 = "v16.1.0";

    @Test
    public void detect_win_doesntLookForAlpine() {
        try (MockedStatic<OS> osMockedStatic = mockStatic(OS.class);
             MockedStatic<Architecture> architectureMockedStatic = mockStatic(Architecture.class)) {

            osMockedStatic.when(OS::guess).thenReturn(OS.Windows);
            architectureMockedStatic.when(Architecture::guess).thenReturn(Architecture.x86);

            Platform platform = Platform.guess();
            assertEquals("win-x86", platform.getNodeClassifier(NODE_VERSION_15));

            verifyNoMoreInteractions(File.class); // doesn't look for a file path
        }
    }

    @Test
    public void detect_arm_mac_download_x64_binary_node15() {
        try (MockedStatic<OS> osMockedStatic = mockStatic(OS.class);
             MockedStatic<Architecture> architectureMockedStatic = mockStatic(Architecture.class)) {

            osMockedStatic.when(OS::guess).thenReturn(OS.Mac);
            architectureMockedStatic.when(Architecture::guess).thenReturn(Architecture.arm64);

            Platform platform = Platform.guess();
            assertEquals("darwin-x64", platform.getNodeClassifier(NODE_VERSION_15));
        }
    }

    @Test
    public void detect_arm_mac_download_x64_binary_node16() {
        try (MockedStatic<OS> osMockedStatic = mockStatic(OS.class);
             MockedStatic<Architecture> architectureMockedStatic = mockStatic(Architecture.class)) {

            osMockedStatic.when(OS::guess).thenReturn(OS.Mac);
            architectureMockedStatic.when(Architecture::guess).thenReturn(Architecture.arm64);

            Platform platform = Platform.guess();
            assertEquals("darwin-arm64", platform.getNodeClassifier(NODE_VERSION_16));
        }
    }

    @Test
    public void detect_linux_notAlpine() throws Exception {

        File alpineRelease = mock(File.class);
        try (MockedStatic<OS> osMockedStatic = mockStatic(OS.class);
             MockedStatic<Architecture> architectureMockedStatic = mockStatic(Architecture.class);
             MockedConstruction<File> mockedConstructionFile = mockConstructionWithAnswer(File.class, invocation -> {
                 if ("/etc/alpine-release".equals(invocation.getArgument(0, String.class))) {
                     return alpineRelease;
                 } else {
                     return invocation.callRealMethod();
                 }
             })) {

            osMockedStatic.when(OS::guess).thenReturn(OS.Linux);
            architectureMockedStatic.when(Architecture::guess).thenReturn(Architecture.x86);

            when(alpineRelease.exists()).thenReturn(false);

            Platform platform = Platform.guess();
            assertEquals("linux-x86", platform.getNodeClassifier(NODE_VERSION_15));
            assertEquals("https://nodejs.org/dist/", platform.getNodeDownloadRoot());
        }
    }

    @Test
    public void detect_linux_alpine() throws Exception {

        File alpineRelease = mock(File.class);
        try (MockedStatic<OS> osMockedStatic = mockStatic(OS.class);
             MockedStatic<Architecture> architectureMockedStatic = mockStatic(Architecture.class);
             MockedConstruction<File> mockedConstructionFile = mockConstructionWithAnswer(File.class, invocation -> {
                 if ("/etc/alpine-release".equals(invocation.getArgument(0, String.class))) {
                     return alpineRelease;
                 } else {
                     return invocation.callRealMethod();
                 }
             })) {

            osMockedStatic.when(OS::guess).thenReturn(OS.Linux);
            architectureMockedStatic.when(Architecture::guess).thenReturn(Architecture.x86);

            when(alpineRelease.exists()).thenReturn(true);

            Platform platform = Platform.guess();
            assertEquals("linux-x86-musl", platform.getNodeClassifier(NODE_VERSION_15));
            assertEquals("https://unofficial-builds.nodejs.org/download/release/",
                    platform.getNodeDownloadRoot());
        }
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
