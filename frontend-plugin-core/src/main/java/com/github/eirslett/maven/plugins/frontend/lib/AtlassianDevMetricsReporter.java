package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricOperatingSystem.getOs;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricType.COUNTER;
import static com.github.eirslett.maven.plugins.frontend.lib.AtlassianDevMetricType.TIME;
import static com.github.eirslett.maven.plugins.frontend.lib.Utils.isBlank;
import static com.google.common.primitives.Ints.checkedCast;
import static java.lang.Boolean.getBoolean;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.time.Instant.now;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Heavily "inspired" from DevMetricsReporter in com.atlassian.maven.extensions:maven-profiler
 */
public class AtlassianDevMetricsReporter  {

    private static final Logger log = getLogger(AtlassianDevMetricsReporter.class);

    private static final ExecutorService EXECUTOR_SERVICE =
            newSingleThreadScheduledExecutor((runnable) -> {
                final Thread thread = new Thread(runnable);
                thread.setDaemon(true); // let it be killed
                thread.setName("dev-metrics-reporter-frontend-maven-plugin");
                return thread;
            });

    private static final HttpClient HTTP_CLIENT = HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom()
                    // timeout when requesting a connection from the connection manager
                    .setConnectionRequestTimeout(checkedCast(SECONDS.toMillis(10)))
                    // timeout until a connection is established
                    .setConnectTimeout(checkedCast(SECONDS.toMillis(5)))
                    // timeout for data / packets to be arrived
                    .setSocketTimeout(checkedCast(SECONDS.toMillis(5)))
                    .build())
            .setMaxConnPerRoute(10)
            .build();
    private static final String METRIC_NAME_PREFIX = "dev.metrics.frontend.maven.plugin.fork.";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String METRICS_ENDPOINT = "https://devmetrics-publisher.prod.atl-paas.net/1/metrics";
    private static final Pattern BUN_VERSION_PATTERN = Pattern.compile("v?(\\d+\\.\\d+).*");

    private static volatile boolean isOffline = false;

    private AtlassianDevMetricsReporter() {
        throw new UnsupportedOperationException("util class");
    }

    private static void sendMetricOverHttp(final Object toSendAsJson) {
        try {
            final HttpPost httpPost = new HttpPost(METRICS_ENDPOINT);
            String jsonString = OBJECT_MAPPER.writeValueAsString(toSendAsJson);
            HttpEntity body = new StringEntity(jsonString, APPLICATION_JSON);
            httpPost.setEntity(body);
            HttpResponse response = HTTP_CLIENT.execute(httpPost);
            if (response.getStatusLine().getStatusCode() >= 300) {
                throw new RuntimeException("Status not OK, was " + response.getStatusLine());
            }
            response.getEntity();
        } catch (Exception e) {
            isOffline = true;
            log.warn("Could not send frontend-maven-plugin dev metric, see debug logs for why");
            log.debug("Failed to send dev metric because:", e);
        }
    }

    private static Map<String, String> getDefaultTags (String artifactId, String forkVersion, String runtimeVersion) {
        boolean isCi = "true".equalsIgnoreCase(getenv("CI")) ||
                "1".equals(getenv("CI")) ||
                getenv().containsKey("bamboo_planKey");

        // Don't let the cardinality get too wild
        Map<String, String> tags = new HashMap<String, String>() {{
            put("dm_devmode", Boolean.toString(getBoolean("atlassian.dev.mode") || getBoolean("jira.dev.mode") || getBoolean("confluence.devmode")));
            put("dm_java_version", getProperty("java.specification.version"));
            put("fork-version", forkVersion);
            put("profiler-artifact-id", artifactId);
            put("profiler-environment", isCi ? "ci" : "local_dev");
            put("profiler-os", getOs());
            put("runtime-version", runtimeVersion);
        }};

        return  tags;
    }

    public static void incrementCount(String name, String artifactId, String forkVersion, String runtimeVersion, Map<String, String> tags) {
        if (isOffline) {
            log.debug("Not reporting count {} because not online!", name);
            return;
        }
        EXECUTOR_SERVICE.submit(() -> {
            tags.putAll(getDefaultTags(artifactId, forkVersion, runtimeVersion));

            AtlassianDevMetric count = new AtlassianDevMetric(
                    COUNTER,
                    METRIC_NAME_PREFIX + name,
                    String.valueOf(1),
                    tags);

            sendMetricOverHttp(count);
        });
    }

    public static class Timer {
        private final Instant start;

        public Timer() {
            this.start = now();
        }

        public void stop(String name, String artifactId, String forkVersion, String runtimeVersion, Map<String, String> tags) {
            if (isOffline) {
                log.debug("Not reporting timer {} because not online!", name);
                return;
            }
            Instant end = now();
            EXECUTOR_SERVICE.submit(() -> {
                tags.put("profiler-cores", Integer.toString(getRuntime().availableProcessors()));
                tags.putAll(getDefaultTags(artifactId, forkVersion, runtimeVersion));

                AtlassianDevMetric timer = new AtlassianDevMetric(
                        TIME,
                        METRIC_NAME_PREFIX + name,
                        String.valueOf(Duration.between(start, end).toMillis()),
                        tags);

                sendMetricOverHttp(timer);
            });
        }
    }

    public static String formatBunVersionForMetric(String bunVersion) {
        Matcher matcher = BUN_VERSION_PATTERN
                .matcher(bunVersion);
        matcher.find();
        return matcher.group(1);
    }

    public static String formatNodeVersionForMetric(String nodeVersion) {
        return nodeVersion.substring(0, nodeVersion.indexOf('.'))
                .replaceFirst("v", "");
    }

    public static String getHostForMetric(String hostSetting, String defaultHost, boolean triedToUsePac, boolean failedToUsePac) {
        String PAC = "packages.atlassian.com";
        String unknown = "unknown";
        List<String> hosts = Arrays.asList(PAC, "github.com", "npmjs.org", "nodejs.org");

        if (!isBlank(hostSetting)) {
            if (triedToUsePac && !failedToUsePac) {
                return PAC;
            }

            for (String candidateHost : hosts) {
                if (hostSetting.contains(candidateHost)) {
                    return candidateHost;
                }
            }

            return unknown;
        }

        for (String candidateHost : hosts) {
            if (defaultHost.contains(candidateHost)) {
                return candidateHost;
            }
        }

        return unknown;
    }
}
