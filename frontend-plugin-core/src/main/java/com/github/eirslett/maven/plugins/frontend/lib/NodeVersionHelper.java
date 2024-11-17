package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;

public class NodeVersionHelper {

    private static final Logger log = getLogger(NodeVersionHelper.class);

    private NodeVersionHelper() {
        throw new UnsupportedOperationException("helper classes should not be instantiated");
    }

    /**
     * Why contain the whole list? So this can work offline, it's a PITA when something doesn't work offline or on a
     * flaky network.
     */
    @VisibleForTesting
    static final Set<String> UNUSUAL_VALID_VERSIONS = Stream.of(
            "latest",
            "latest-argon",
            "latest-boron",
            "latest-carbon",
            "latest-dubnium",
            "latest-erbium",
            "latest-fermium",
            "latest-gallium",
            "latest-hydrogen",
            "latest-iron",

            // future releases
            "latest-jod",
            "latest-krypton",
            "latest-lithium",
            "latest-magnesium",
            "latest-neon",
            "latest-oxygen",
            "latest-platinum",

            "latest-v0.10.x",
            "latest-v0.12.x",
            "latest-v10.x",
            "latest-v11.x",
            "latest-v12.x",
            "latest-v13.x",
            "latest-v14.x",
            "latest-v15.x",
            "latest-v16.x",
            "latest-v17.x",
            "latest-v18.x",
            "latest-v19.x",
            "latest-v20.x",
            "latest-v21.x",
            "latest-v22.x",
            "latest-v23.x",
            "latest-v24.x",
            "latest-v25.x",
            "latest-v26.x",
            "latest-v27.x",
            "latest-v28.x",
            "latest-v4.x",
            "latest-v5.x",
            "latest-v6.x",
            "latest-v7.x",
            "latest-v8.x",
            "latest-v9.x",
            "v0.10.16-isaacs-manual",
            "node-0.0.1",
            "node-0.0.2",
            "node-0.0.3",
            "node-0.0.4",
            "node-0.0.5",
            "node-0.0.6",
            "node-0.1.0",
            "node-0.1.1",
            "node-0.1.10",
            "node-0.1.11",
            "node-0.1.12",
            "node-0.1.13",
            "node-0.1.2",
            "node-0.1.3",
            "node-0.1.4",
            "node-0.1.5",
            "node-0.1.6",
            "node-0.1.7",
            "node-0.1.8",
            "node-0.1.9",
            "node-latest",
            "node-v0.1.100",
            "node-v0.1.101",
            "node-v0.1.102",
            "node-v0.1.103",
            "node-v0.1.104",
            "node-v0.1.14",
            "node-v0.1.15",
            "node-v0.1.16",
            "node-v0.1.17",
            "node-v0.1.18",
            "node-v0.1.19",
            "node-v0.1.20",
            "node-v0.1.21",
            "node-v0.1.22",
            "node-v0.1.23",
            "node-v0.1.24",
            "node-v0.1.25",
            "node-v0.1.26",
            "node-v0.1.27",
            "node-v0.1.28",
            "node-v0.1.29",
            "node-v0.1.30",
            "node-v0.1.31",
            "node-v0.1.32",
            "node-v0.1.33",
            "node-v0.1.90",
            "node-v0.1.91",
            "node-v0.1.92",
            "node-v0.1.93",
            "node-v0.1.94",
            "node-v0.1.95",
            "node-v0.1.96",
            "node-v0.1.97",
            "node-v0.1.98",
            "node-v0.1.99",
            "node-v0.10.14",
            "node-v0.2.0",
            "node-v0.2.1",
            "node-v0.2.2",
            "node-v0.2.3",
            "node-v0.2.4",
            "node-v0.2.5",
            "node-v0.2.6",
            "node-v0.3.0",
            "node-v0.3.1",
            "node-v0.3.2",
            "node-v0.3.3",
            "node-v0.3.4",
            "node-v0.3.5",
            "node-v0.3.6",
            "node-v0.3.7",
            "node-v0.3.8",
            "node-v0.4.0",
            "node-v0.4.1",
            "node-v0.4.10",
            "node-v0.4.11",
            "node-v0.4.12",
            "node-v0.4.2",
            "node-v0.4.3",
            "node-v0.4.4",
            "node-v0.4.5",
            "node-v0.4.6",
            "node-v0.4.7",
            "node-v0.4.8",
            "node-v0.4.9",
            "node-v0.4",
            "node-v0.5.0",
            "node-v0.6.1",
            "node-v0.6.10",
            "node-v0.6.11",
            "node-v0.6.12",
            "node-v0.6.13",
            "node-v0.6.2",
            "node-v0.6.3",
            "node-v0.6.4",
            "node-v0.6.5",
            "node-v0.6.6",
            "node-v0.6.7",
            "node-v0.6.8",
            "node-v0.6.9"
    ).collect(toSet());

    @VisibleForTesting
    static final Pattern VALID_VERSION_PATTERN = Pattern.compile("^v?\\d*\\.\\d*\\.\\d*$");

    private static final String LATEST_OF_GIVEN_MAJOR_FORMAT = "latest-v%s.x";
    private static final Pattern MAJOR_VERSION_ONLY_PATTERN = Pattern.compile("^\\d+$");

    /**
     * We cache this to prevent multiple network requests in a multi-module Maven build, it's very unlikely a version
     * someone is released suddenly halfway through the build and that a consumer has actually asked for that
     */
    @VisibleForTesting
    static final AtomicReference<Optional<List<NodeVersion>>> nodeVersions = new AtomicReference<>(empty());

    public static boolean validateVersion(String version) {
        if (UNUSUAL_VALID_VERSIONS.contains(version)) {
            return true;
        }

        version = version.replaceFirst("v", ""); // we're about to add it back
        if (MAJOR_VERSION_ONLY_PATTERN.matcher(version).find()) {
            String latestOfMajorVersion = format(LATEST_OF_GIVEN_MAJOR_FORMAT, version).toLowerCase();
            return UNUSUAL_VALID_VERSIONS.contains(latestOfMajorVersion);
        }

        Matcher matcher = VALID_VERSION_PATTERN.matcher(version);
        return matcher.find();
    }

    public static String getDownloadableVersion(String version) {
        version = version.toLowerCase(); // all the versions seem to be lower case

        if (UNUSUAL_VALID_VERSIONS.contains(version)) {
            return version;
        }

        version = version.replaceFirst("v", ""); // we're about to add it back

        return findMatchingReleasedVersion(version)
                .orElse("v" + version);
    }

    public static Optional<String> findMatchingReleasedVersion(String requestedVersionLowercaseWithoutLeadingV) {
        if (!nodeVersions.get().isPresent()) {
            synchronized (NodeVersionHelper.class) { // avoiding racing sending multiple requests
                try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                    HttpGet getNodeVersionIndex = new HttpGet("https://nodejs.org/dist/index.json");
                    try (CloseableHttpResponse response = httpClient.execute(getNodeVersionIndex)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode != 200) {
                            throw new RuntimeException(format("Response status was not 200, was: %s and body was:\n%s",
                                    statusCode, response.getEntity().toString()));
                        }

                        String contentType = response.getEntity().getContentType().getValue();
                        if (!"application/json".equals(contentType)) {
                            throw new RuntimeException("Response content type was not JSON, was: " + contentType);
                        }

                        ObjectMapper relaxedObjectMapper =
                                new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
                        nodeVersions.set(Optional.of(relaxedObjectMapper
                                .readValue(response.getEntity().getContent(), new TypeReference<List<NodeVersion>>() {})));
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch the list of released node versions " +
                            "to turn loosely-defined versions and into something " +
                            "specific & downloadable");
                    log.debug("Failed to download list of node versions", e);
                    nodeVersions.set(Optional.of(emptyList()));
                }
            }
        }

        String versionToLookFor = "v" + requestedVersionLowercaseWithoutLeadingV;
        return nodeVersions.get().get().stream()
                .map(NodeVersion::getVersion)
                .sorted(new NodeVersionComparator()
                        .reversed()) // we want the newest version to appear first
                .filter(listedVersion -> listedVersion.startsWith(versionToLookFor))
                .findFirst();
    }

    /**
     * When I grow up, I want to be a Java record class!
     */
    private static class NodeVersion {
        public String version;

        public NodeVersion() {}

        public String getVersion() {
            return version;
        }
    }

    @VisibleForTesting
    static class NodeVersionComparator implements Comparator<String> {
        @Override
        public int compare(String firstVersion, String secondVersion) {
            firstVersion = firstVersion.replaceFirst("v", "");
            secondVersion = secondVersion.replaceFirst("v", "");

            List<String> firstVersionParts = asList(firstVersion.split("\\."));
            List<String> secondVersionParts = asList(secondVersion.split("\\."));

            for (int partsIndex = 0; partsIndex < firstVersionParts.size(); partsIndex++) {
                int delta = Integer.parseInt(firstVersionParts.get(partsIndex))
                        - Integer.parseInt(secondVersionParts.get(partsIndex));

                // handle the same version appearing twice
                if (delta == 0 && partsIndex != firstVersionParts.size() -1) {
                    continue;
                }

                return delta;
            }

            throw new RuntimeException("Unexpectedly couldn't sort released node versions. Raise a bug report");
        }
    }
}
