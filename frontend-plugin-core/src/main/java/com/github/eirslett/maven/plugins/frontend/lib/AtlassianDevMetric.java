package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Harvested from {@code Metric} in cloud.atlassian.logmon:dev-metrics-api
 */
class AtlassianDevMetric {
    private String name;
    private AtlassianDevMetricType type;
    private String value;
    private Map<String, String> tags;
    private final Map<String, String> metatags = null;

    AtlassianDevMetric(final AtlassianDevMetricType type, final String name, final String value, final Map<String, String> tags) {
        this.type = requireNonNull(type);
        this.name = requireNonNull(name);
        this.value = value;
        this.tags = requireNonNull(tags);
    }

    public String getName() {
        return name;
    }

    public AtlassianDevMetricType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<String, String> getMetatags() {
        return metatags;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", value=" + value +
                ", tags=" + tags +
                ", metatags=" + metatags +
                '}';
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof AtlassianDevMetric)) {
            return false;
        }
        final AtlassianDevMetric otherMetric = (AtlassianDevMetric) otherObject;
        return Objects.equals(name, otherMetric.name) &&
                type == otherMetric.type &&
                Objects.equals(value, otherMetric.value) &&
                Objects.equals(tags, otherMetric.tags) &&
                Objects.equals(metatags, otherMetric.metatags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, value, tags, metatags);
    }
}
