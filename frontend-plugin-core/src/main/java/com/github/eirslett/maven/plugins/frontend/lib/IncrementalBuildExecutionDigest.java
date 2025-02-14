package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class IncrementalBuildExecutionDigest {

    /**
     * This should be incremented as soon as the digest schema or semantics change
     */
    public static final Long CURRENT_DIGEST_VERSION = 2L;

    private static final String SERIALIZATION_CHARSET = UTF_8.toString();
    /**
     * Must be something that would be encoded otherwise we could end up with a set of
     * parts we don't expect.
     */
    static final String SERIALIZATION_SEPARATOR = ";";

    public Long digestVersion;

    @JsonDeserialize(keyUsing = ExecutionCoordinatesDeserializer.class)
    @JsonSerialize(keyUsing = ExecutionCoordinatesSerializer.class)
    public Map<ExecutionCoordinates, Execution> executions;

    public IncrementalBuildExecutionDigest() {
            // for Jackson
    }

    public IncrementalBuildExecutionDigest(Long digestVersion, Map<ExecutionCoordinates, Execution> executions) {
        this.digestVersion = digestVersion;
        this.executions = executions;
    }

    public static class ExecutionCoordinates {
        public String goal;
        public String id;
        public String lifecyclePhase;

        public ExecutionCoordinates() {
            // for Jackson
        }

        public ExecutionCoordinates(String goal, String id, String lifecyclePhase) {
            this.goal = goal;
            this.id = id;
            this.lifecyclePhase = lifecyclePhase;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ExecutionCoordinates)) return false;
            ExecutionCoordinates that = (ExecutionCoordinates) o;
            return Objects.equals(goal, that.goal) && Objects.equals(id, that.id) && Objects.equals(lifecyclePhase, that.lifecyclePhase);
        }

        @Override
        public int hashCode() {
            return Objects.hash(goal, id, lifecyclePhase);
        }
    }

    public static class Execution {
        public String arguments;
        public Map<String, String> environmentVariables;
        public Set<File> files;
        public Runtime runtime;
        public Long millisecondsSaved = 0L;

        public Execution() {
            // for Jackson
        }

        public Execution(String arguments, Map<String, String> environmentVariables, Set<File> files, Runtime runtime) {
            this.files = files;
            this.environmentVariables = environmentVariables;
            this.arguments = arguments;
            this.runtime = runtime;
        }

        public static class File {
            public String filename;
            public Integer byteLength;
            public String hash;

            public File() {
                // for Jackson
            }

            public File(String filename, Integer byteLength, String hash) {
                this.filename = filename;
                this.byteLength = byteLength;
                this.hash = hash;
            }

            @Override
            public String toString() {
                return "File{" +
                        "filename='" + filename + '\'' +
                        ", byteLength=" + byteLength +
                        ", hash='" + hash + '\'' +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof File)) return false;
                File file = (File) o;
                return Objects.equals(filename, file.filename) && Objects.equals(byteLength, file.byteLength) && Objects.equals(hash, file.hash);
            }

            @Override
            public int hashCode() {
                return Objects.hash(filename, byteLength, hash);
            }
        }

        public static class Runtime {
            public String runtime;
            public String runtimeVersion;

            public Runtime() {
                // for Jackson
            }

            public Runtime(String runtime, String runtimeVersion) {
                this.runtime = runtime;
                this.runtimeVersion = runtimeVersion;
            }

            @Override
            public String toString() {
                return "Runtime{" +
                        "runtime='" + runtime + '\'' +
                        ", runtimeVersion='" + runtimeVersion + '\'' +
                        '}';
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Runtime)) return false;
                Runtime runtime = (Runtime) o;
                return Objects.equals(this.runtime, runtime.runtime) && Objects.equals(runtimeVersion, runtime.runtimeVersion);
            }

            @Override
            public int hashCode() {
                return Objects.hash(runtime, runtimeVersion);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Execution)) return false;
            Execution execution = (Execution) o;
            return Objects.equals(files, execution.files) && Objects.equals(environmentVariables, execution.environmentVariables) && Objects.equals(arguments, execution.arguments) && Objects.equals(runtime, execution.runtime) && Objects.equals(millisecondsSaved, execution.millisecondsSaved);
        }

        @Override
        public int hashCode() {
            return Objects.hash(files, environmentVariables, arguments, runtime, millisecondsSaved);
        }
    }

    public static class ExecutionCoordinatesSerializer extends StdSerializer<ExecutionCoordinates> {

        public ExecutionCoordinatesSerializer() {
            super(IncrementalBuildExecutionDigest.ExecutionCoordinates.class);
        }

        @Override
        public void serialize(IncrementalBuildExecutionDigest.ExecutionCoordinates value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            final String encoded = URLEncoder.encode(value.goal, SERIALIZATION_CHARSET)
                            + SERIALIZATION_SEPARATOR + URLEncoder.encode(value.id, SERIALIZATION_CHARSET)
                            + SERIALIZATION_SEPARATOR + URLEncoder.encode(value.lifecyclePhase, SERIALIZATION_CHARSET);
            gen.writeFieldName(encoded);
        }
    }

    public static class ExecutionCoordinatesDeserializer extends KeyDeserializer {

        public ExecutionCoordinatesDeserializer() {
            super();
        }

        @Override
        public IncrementalBuildExecutionDigest.ExecutionCoordinates deserializeKey(String key, DeserializationContext context) throws UnsupportedEncodingException {
            requireNonNull(key, "ExecutionCoordinates string cannot be null");
            List<String> keyParts = asList(key.split(SERIALIZATION_SEPARATOR));
            if (keyParts.size() != 3) {
                throw new IllegalArgumentException("Supplied ExecutionCoordinates key didn't have three parts, was: " + key);
            }

            final ExecutionCoordinates executionCoordinates = new ExecutionCoordinates();
            executionCoordinates.goal = URLDecoder.decode(keyParts.get(0), SERIALIZATION_CHARSET);
            executionCoordinates.id = URLDecoder.decode(keyParts.get(1), SERIALIZATION_CHARSET);
            executionCoordinates.lifecyclePhase = URLDecoder.decode(keyParts.get(2), SERIALIZATION_CHARSET);

            return executionCoordinates;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IncrementalBuildExecutionDigest)) return false;
        IncrementalBuildExecutionDigest that = (IncrementalBuildExecutionDigest) o;
        return Objects.equals(digestVersion, that.digestVersion) && Objects.equals(executions, that.executions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(digestVersion, executions);
    }
}
