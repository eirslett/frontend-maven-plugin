package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.File;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.ExecutionCoordinates;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;

import static com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.SERIALIZATION_SEPARATOR;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IncrementalBuildExecutionDigestTest {

    /**
     * De/serialization in Jackson is not trivial and a bunch of stuff can blow up at runtime.
     */
    @Test
    public void serializationSmokeTest() throws Exception {
        IncrementalBuildExecutionDigest digestToSerialize = new IncrementalBuildExecutionDigest(
                2L,
                singletonMap(
                        new ExecutionCoordinates(
                                "test-goal",
                                "test-id"
                                        // make sure we got a good character
                                        + SERIALIZATION_SEPARATOR,
                                "test-phase"),
                        new Execution(
                                "test-arguments",
                                Collections.singletonMap("NODE_ENV", "test"),
                                new HashSet<>(singletonList(new File("test-file.js", 12345, "abc123"))),
                                new Runtime("node", "{\n" +
                                        "  '@atlassian/solicitorio': '3.4.0',\n" +
                                        "  npm: '8.15.0',\n" +
                                        "  node: '18.17.0',\n" +
                                        "  acorn: '8.8.2',\n" +
                                        "  ada: '2.5.0',\n" +
                                        "  ares: '1.19.1',\n" +
                                        "  brotli: '1.0.9',\n" +
                                        "  cldr: '43.0',\n" +
                                        "  icu: '73.1',\n" +
                                        "  llhttp: '6.0.11',\n" +
                                        "  modules: '108',\n" +
                                        "  napi: '9',\n" +
                                        "  nghttp2: '1.52.0',\n" +
                                        "  nghttp3: '0.7.0',\n" +
                                        "  ngtcp2: '0.8.1',\n" +
                                        "  openssl: '3.0.9+quic',\n" +
                                        "  simdutf: '3.2.12',\n" +
                                        "  tz: '2023c',\n" +
                                        "  undici: '5.22.1',\n" +
                                        "  unicode: '15.0',\n" +
                                        "  uv: '1.44.2',\n" +
                                        "  uvwasi: '0.0.18',\n" +
                                        "  v8: '10.2.154.26-node.26',\n" +
                                        "  zlib: '1.2.13.1-motley'\n" +
                                        "}"))));

        ObjectMapper objectMapper = IncrementalMojoHelper.OBJECT_MAPPER;

        String jsonString = objectMapper.writeValueAsString(digestToSerialize);
        IncrementalBuildExecutionDigest deserializedDigest = objectMapper.readValue(jsonString, IncrementalBuildExecutionDigest.class);

        assertEquals(digestToSerialize, deserializedDigest);
    }
}
