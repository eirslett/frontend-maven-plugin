package com.github.eirslett.maven.plugins.frontend.lib;

import org.apache.commons.codec.digest.MurmurHash3;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

public class IncrementalMojoHelper {
    private static final Logger log = getLogger(IncrementalMojoHelper.class);
    private final File workingDirectory;
    private final boolean isActive;

    public IncrementalMojoHelper(String activationFlag, File workingDirectory) {
        this.workingDirectory = workingDirectory;

        this.isActive = activationFlag != null && activationFlag.equals("true");
    }

    public boolean shouldExecute() {
        if (!isActive) {
            return true;
        }

        try {
            ArrayList<File> digestFiles = getDigestFiles();
            String currDigest = createDigest(digestFiles);

            try {
                String prevDigest = readPreviousDigest();

                if (currDigest.equals(prevDigest)) {
                    log.info("Atlassian Fork FTW - No changes detected! - Skipping execution");
                    // For now, we'll just assume all the target files are still there ;)
                    return false; // TADAM! Build is not needed
                }

                reportDigestDifferences(prevDigest, currDigest);
            } catch (FileNotFoundException e) {
                // This is fine... we'll save the digest this time
            } // Let any other IOException's get handled below

            saveDigestCandidate(currDigest);
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

        log.debug("Accepting yarn incremental build digest...");
        if (getDigestFile().exists()) {
            if (!getDigestFile().delete()) {
                log.warn("Failed to delete the previous incremental build digest");
            }
        }

        if (!getDigestCandidateFile().renameTo(getDigestFile())) {
            log.warn("Failed to accept the incremental build digest");
        }
    }

    private ArrayList<File> getDigestFiles() throws IOException {
        IncrementalVisitor visitor = new IncrementalVisitor();

        Files.walkFileTree(workingDirectory.toPath(), visitor);

        return visitor.getFiles();
    }

    static class IncrementalVisitor extends SimpleFileVisitor<Path> {
        static final Set<String> IGNORED_DIRS = new HashSet<>(asList(
                "build",
                "dist",
                "target"
        ));

        static final Set<String> DIGEST_EXTENSIONS = new HashSet<>(asList(
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
        static final Set<String> DIGEST_FILES = new HashSet<>(asList(
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

        private final ArrayList<File> files = new ArrayList<>();

        @Override
        public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs)
        {
            if (IGNORED_DIRS.contains(file.getFileName().toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            } else {
                return FileVisitResult.CONTINUE;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String fileName = file.getFileName().toString();

            if (DIGEST_FILES.contains(fileName)) {
                files.add(file.toFile());
            } else {
                String extension = getFileExtension(fileName);

                if (extension != null) {
                    if (DIGEST_EXTENSIONS.contains(extension)) {
                        files.add(file.toFile());
                    }
                }
            }

            return FileVisitResult.CONTINUE;
        }

        public ArrayList<File> getFiles() {
            return files;
        }

        static private String getFileExtension(String fileName) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                return fileName.substring(dotIndex + 1);
            } else {
                return null;
            }
        }
    }

    private static String createDigest(ArrayList<File> digestFiles) {
        return createFilesDigest(digestFiles)
                + createToolsDigest()
                + createEnvironmentDigest();
    }

    private static String createFilesDigest(ArrayList<File> digestFiles) {
        // Why not use parallelStream()? Well testing on JSM DC's
        // node_modules folders which are the worst case in DC shows 2s vs 3s
        // but for Stash and Jira SW it's faster to be single threaded. We might
        // as well take the single threaded-ness since this could be running in a
        // Maven reactor leading to too much parallelism fighting itself.
        return digestFiles.stream()
                .map(file -> {
                    try {
                        byte[] fileBytes = Files.readAllBytes(file.toPath());
                        // Requirements for hash function: 1 - single byte change is
                        // highly likely to result in a different hash, 2 - fast, baby fast!
                        long[] hash = MurmurHash3.hash128x64(fileBytes);
                        return file + ":" + fileBytes.length + ":" +  Arrays.toString(hash);
                    } catch (IOException exception) {
                        throw new RuntimeException(format("Failed to read file: %s", file), exception);
                    }
                })
                .sorted()
                .collect(joining("\n"));
    }

    private static String createToolsDigest() {
        String[][] commands = {
                {"node", "--version"},
                {"yarn", "--version"},
                {"npm", "--version"}
        };

        return stream(commands)
                .parallel()
                .map(IncrementalMojoHelper::createCommandDigest)
                .sorted()
                .collect(joining());
    }

    private static String createCommandDigest(String... command) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ");
        for (int i = 0; i < command.length; i++) {
            sb.append(command[i]);
            if (i < command.length - 1) {
                sb.append(" ");
            }
        }
        sb.append("\n");

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Redirect error stream to standard output

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append("# ").append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            sb.append("# ").append("exit code: ").append(exitCode).append("\n");
        } catch (InterruptedException e) {
            sb.append("# ").append("!interrupted: ").append(e.toString().replace("\n", " ")).append("\n");
        } catch (IOException e) {
            sb.append("# ").append("!io: ").append(e.toString().replace("\n", " ")).append("\n");
        }

        return sb.toString();
    }

    private static String createEnvironmentDigest() {
        List<String> envVars = asList(
                "NODE_ENV",
                "BABEL_ENV",
                "OS",
                "OS_VERSION",
                "OS_ARCH",
                "OS_NAME",
                "OS_FAMILY"
        );

        return envVars.stream()
                .map(key -> format("# %s = %s", key, System.getenv(key)))
                .map(entry -> entry.replaceAll("\n", " "))
                .collect(joining("\n"));
    }

    private void saveDigestCandidate(String currDigest) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getDigestCandidateFile()))) {
            writer.write(currDigest);
        }
    }

    private String readPreviousDigest() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(getDigestFile()))) {
            StringBuilder previousDigest = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                previousDigest.append(line).append("\n");
            }

            return previousDigest.toString();
        }
    }

    private void reportDigestDifferences(String prevDigest, String currDigest) {
        Map<String, String> prevDigestContents = getDigestFilesMap(prevDigest);
        Map<String, String> currDigestContents = getDigestFilesMap(currDigest);

        for (Map.Entry<String, String> entry : prevDigestContents.entrySet()) {
            String prevFile = entry.getKey();
            String prevHash = entry.getValue();
            String currHash = currDigestContents.get(prevFile);
            if (currHash == null) {
                log.debug("File removed: {}", prevFile);
            } else if (!prevHash.equals(currHash)) {
                log.debug("File changed: {}", prevFile);
            }
        }

        for (Map.Entry<String, String> entry : currDigestContents.entrySet()) {
            String currFile = entry.getKey();
            String prevHash = prevDigestContents.get(currFile);
            if (prevHash == null) {
                log.debug("File added: {}", currFile);
            }
        }
    }

    private Map<String, String> getDigestFilesMap(String digest) {
        return stream(digest.split("\n"))
                .filter(line -> !line.startsWith("#"))
                .map(line -> line.split(" : "))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

    private File getDigestCandidateFile() {
        return new File(getTargetDir(), "yarn-incremental-build-digest.candidate.txt");
    }

    private File getDigestFile() {
        return new File(getTargetDir(), "yarn-incremental-build-digest.txt");
    }

    private File getTargetDir() {
        return new File(workingDirectory, "target");
    }
}
