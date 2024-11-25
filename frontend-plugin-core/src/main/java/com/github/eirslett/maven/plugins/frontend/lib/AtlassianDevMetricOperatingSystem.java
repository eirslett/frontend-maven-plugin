package com.github.eirslett.maven.plugins.frontend.lib;

import static java.lang.System.getProperty;

/**
 * Harvested from {@code AbstractDevMetricsPublisher#OperatingSystem} in cloud.atlassian.logmon:dev-metrics-impl
 */
class  AtlassianDevMetricOperatingSystem{
    static String getOs() {
        return getOsType().toString();
    }

    private enum OSType {
        Windows, MacOS, Linux, Other
    }

    private static OSType getOsType() {
        final String OS = getProperty("os.name", "generic").toLowerCase();

        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            return OSType.MacOS;
        }
        if (OS.contains("nux")) {
            return OSType.Linux;
        }
        if (OS.contains("win")) {
            return OSType.Windows;
        }

        return OSType.Other;
    }
}
