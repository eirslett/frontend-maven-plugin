package com.github.eirslett.maven.plugins.frontend.lib;

public enum AtlassianDevMetricsIncremental {
    NOT_ENABLED,
    /**
     * Incremental builds were enabled, but we had to run the build because we haven't
     * done it yet or one of the source files has changed
     */
    BUILT,
    /**
     * Incremental builds are enabled, and we were able to skip rebuilding
     */
    REBUILDING_SKIPPED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
