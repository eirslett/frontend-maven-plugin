package com.github.eirslett.maven.plugins.frontend.lib;

/**
 * From least to most work required
 */
public enum AtlassianDevMetricsInstallationWork {
    /**
     * For whatever reason we couldn't track how much work went on
     */
    UNKNOWN,

    /**
     * Already provided by some other binary
     */
    PROVIDED,
    /*
     * Already installed, no work required
     */
    INSTALLED,
    /**
     * Binary cached, but needed to be installed
     */
    CACHED,
    /**
     * Had to go off and download the binaries
     */
    DOWNLOADED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
