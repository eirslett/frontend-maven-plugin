package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.File;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.ExecutionCoordinates;
import org.junit.jupiter.api.Test;

import java.util.Collections;

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
                                singletonList(new File("test-file.js", 12345, "abc123")),
                                new Runtime("node", "14.17.0"))));

        ObjectMapper objectMapper = IncrementalMojoHelper.OBJECT_MAPPER;

        String jsonString = objectMapper.writeValueAsString(digestToSerialize);
        IncrementalBuildExecutionDigest deserializedDigest = objectMapper.readValue(jsonString, IncrementalBuildExecutionDigest.class);

        assertEquals(digestToSerialize, deserializedDigest);
    }
}
