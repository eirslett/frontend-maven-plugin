package com.github.eirslett.maven.plugins.frontend.lib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.Execution.Runtime;
import com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.ExecutionCoordinates;
import org.apache.commons.codec.digest.MurmurHash3;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.github.eirslett.maven.plugins.frontend.lib.IncrementalBuildExecutionDigest.CURRENT_DIGEST_VERSION;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;

public class IncrementalMojoHelper {
    private static final Logger log = getLogger(IncrementalMojoHelper.class);
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            // Allow for reading without blowing up
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final File targetDirectory;
    private final File workingDirectory;
    private final boolean isActive;

    private IncrementalBuildExecutionDigest digest;

    public IncrementalMojoHelper(String activationFlag, File targetDirectory, File workingDirectory) {
        this.targetDirectory = targetDirectory;
        this.workingDirectory = workingDirectory;

        this.isActive = "true".equals(activationFlag);
    }

    public boolean shouldExecute(String arguments, ExecutionCoordinates coordinates, Optional<Runtime> runtime, Map<String, String> suppliedEnvVars) {
        if (!isActive) {
            return true;
        }

        if (!runtime.isPresent()) {
            log.warn("Failed to do incremental compilation because the runtime version couldn't be fetched, see the debug logs");
            return true;
        }

        try {
            File digestFileLocation = getDigestFile();
            if (digestFileLocation.exists()) {
                digest = readDigest(digestFileLocation);
            }

            if (isNull(digest)) {
                digest = new IncrementalBuildExecutionDigest(CURRENT_DIGEST_VERSION, new HashMap<>());
            }

            boolean digestVersionsMatch = Objects.equals(digest.digestVersion, CURRENT_DIGEST_VERSION);

            Execution thisExecution = new Execution(
                    arguments,
                    getAllEnvVars(suppliedEnvVars),
                    createFilesDigest(),
                    runtime.get());

            boolean canSkipExecution = false;
            if (digestVersionsMatch) {
                Execution previousExecution = digest.executions.get(coordinates);
                canSkipExecution = Objects.equals(previousExecution, thisExecution);
            }

            if (canSkipExecution) {
                log.info("Atlassian Fork FTW - No changes detected! - Skipping execution");
            }

            digest.executions.put(coordinates, thisExecution);

            return !canSkipExecution;
        } catch (Exception exception) {
            log.error("Failure while determining if an incremental build is needed. See debug logs");
            log.debug("Failure while determining if an incremental build was...", exception);
        }

        return true;
    }

    public void acceptIncrementalBuildDigest() {
        if (!isActive) {
            return;
        }

        try {
            log.debug("Accepting the incremental build digest...");
            if (getDigestFile().exists()) {
                if (!getDigestFile().delete()) {
                    log.warn("Failed to delete the previous incremental build digest");
                }
            }

            saveDigest(digest);
        } catch (Exception exception) {
            log.warn("Failed to save the incremental build digest, see the debug logs");
            log.debug("Failed to save the incremental build digest, because: ", exception);
        }
    }

    static class IncrementalVisitor extends SimpleFileVisitor<Path> {
        private final Set<Execution.File> files;

        public IncrementalVisitor(Set<Execution.File> files) {
            this.files = files;
        }

        /**
         * It's ever so slightly faster for this to be a {@link List} instead of a
         * {@link HashSet}, a couple more elements would tip it over.
         */
        private static final List<String> IGNORED_DIRS = asList(
                "build",
                "dist",
                "target"
        );

        private static final Set<String> DIGEST_EXTENSIONS = new HashSet<>(asList(
                // JS
                "js",
                "jsx",
                "cjs",
                "mjs",
                "ts",
                "tsx",
                // CSS
                "css",
                "scss",
                "sass",
                "less",
                "styl",
                "stylus",
                // templates
                "ejs",
                "hbs",
                "handlebars",
                "pug",
                "soy",
                "html",
                "vm",
                "vmd",
                "vtl",
                "ftl",
                // config
                "json",
                "xml",
                "yaml",
                "yml",
                "csv",
                "lock",
                // Images
                "apng",
                "png",
                "jpg",
                "jpeg",
                "gif",
                "webp",
                "svg",
                "ico",
                "bmp",
                "tiff",
                "tif",
                "avif",
                "eps",
                // Fonts
                "ttf",
                "otf",
                "woff",
                "woff2",
                "eot",
                "sfnt",
                // Audio and Video
                "mp3",
                "mp4",
                "webm",
                "wav",
                "flac",
                "aac",
                "ogg",
                "oga",
                "opus",
                "m4a",
                "m4v",
                "mov",
                "avi",
                "wmv",
                "flv",
                "mkv",
                "flac"
        ));

        // Files that are to be included in the digest but are not of the above extensions
        private static final Set<String> DIGEST_FILES = new HashSet<>(asList(
                ".parcelrc",
                ".babelrc",
                ".eslintrc",
                ".eslintignore",
                ".prettierrc",
                ".prettierignore",
                ".stylelintrc",
                ".stylelintignore",
                ".browserslistrc",
                ".npmrc"
        ));

        @Override
        public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) {
            if (IGNORED_DIRS.contains(file.getFileName().toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            return FileVisitResult.CONTINUE;
        }

        /**
         * PERF NOTES:
         * <ul>
         *     <li>Yes, we're mixing the walking with the work,  but due to the
         *     underlying library calls, we're 300ms better off in JSM DC</li>
         *     <li>Yes, this removes parallelism, but even in JSM DC (worst case),
         *     it's negligible, while for Stash and Jira DC it's faster to be single
         *     threaded. Combined with these all usually being run in Maven
         *     reactors, this is more likely to pay off by allowing for better
         *     overall parallelism</li>
         * </ul>
         */
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String fileName = file.getFileName().toString();

            if (DIGEST_FILES.contains(fileName) ||
                    DIGEST_EXTENSIONS.contains(getFileExtension(fileName))) {
                try {
                    byte[] fileBytes = Files.readAllBytes(file);
                    // Requirements for hash function: 1 - single byte change is
                    // highly likely to result in a different hash, 2 - fast, baby fast!
                    long[] hash = MurmurHash3.hash128x64(fileBytes);
                    String hashString = Arrays.toString(hash);
                    files.add(new Execution.File(file.toString(), fileBytes.length, hashString));
                } catch (IOException exception) {
                    throw new RuntimeException(format("Failed to read file: %s", file), exception);
                }
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            log.debug("Failed to visit {}", path, e);
            return super.visitFileFailed(path, e);
        }

        private static String getFileExtension(String fileName) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 // skip over dot-files like .babelrc
                    // check if the '.' is the last character == no extension
                    && dotIndex < fileName.length() - 1) {
                return fileName.substring(dotIndex + 1);
            } else {
                return null;
            }
        }
    }

    private Set<Execution.File> createFilesDigest() throws IOException {
        final Set<Execution.File> files = new HashSet<>();

        IncrementalVisitor visitor = new IncrementalVisitor(files);
        Files.walkFileTree(workingDirectory.toPath(), visitor);

        return files;
    }

    private static Map<String, String> getAllEnvVars(Map<String, String> userDefinedEnvVars) {
        final  Map<String, String> effectiveEnvVars = new HashMap<>();

        List<String> defaultEnvVars = asList(
                "NODE_ENV",
                "BABEL_ENV",
                "OS",
                "OS_VERSION",
                "OS_ARCH",
                "OS_NAME",
                "OS_FAMILY"
        );
        defaultEnvVars.forEach(envVarKey -> {
            String envVarValue = System.getenv(envVarKey);
            effectiveEnvVars.put(envVarKey, nullStringIsEmpty(envVarValue));
        });

        if (userDefinedEnvVars != null) {
            // These would override our defaults
            effectiveEnvVars.putAll(userDefinedEnvVars);
        }

        return effectiveEnvVars;
    }

    /**
     * Most stuff treats empty and unset as the same
     */
    private static String nullStringIsEmpty(String string) {
        if (isNull(string)) {
            return "";
        }
        return string;
    }

    private void saveDigest(IncrementalBuildExecutionDigest digest) throws IOException {
        OBJECT_MAPPER.writeValue(getDigestFile(), digest);
    }

    private IncrementalBuildExecutionDigest readDigest(File digest) throws IOException {
        return OBJECT_MAPPER.readValue(digest, IncrementalBuildExecutionDigest.class);
    }

    private File getDigestFile() {
        return new File(targetDirectory, "yarn-incremental-build-digest.json");
    }
}
