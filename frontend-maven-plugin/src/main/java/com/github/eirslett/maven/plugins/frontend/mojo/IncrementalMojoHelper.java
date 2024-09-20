package com.github.eirslett.maven.plugins.frontend.mojo;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IncrementalMojoHelper {
    private final File workingDirectory;
    private final Log log;
    private final boolean isActive;

    public IncrementalMojoHelper(String activationFlag, File workingDirectory, Log log) {
        this.workingDirectory = workingDirectory;
        this.log = log;

        this.isActive = activationFlag.equals("true");
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
                    getLog().info("Atlassian Fork FTW - No changes detected! - Skipping yarn build");
                    // For now, we'll just assume all the target files are still there ;)
                    return false; // TADAM! Build is not needed
                }

                reportDigestDifferences(prevDigest, currDigest);
            } catch (FileNotFoundException e) {
                // This is fine... we'll save the digest this time
            } // Let any other IOException's get handled below

            saveDigestCandidate(currDigest);
        } catch (IOException e) {
            getLog().error("Failed to determine if an incremental build is needed: " + e);
        }

        return true;
    }

    public void acceptIncrementalBuildDigest() {
        if (!isActive) {
            return;
        }

        getLog().info("Accepting yarn incremental build digest...");
        if (getDigestFile().exists()) {
            if (!getDigestFile().delete()) {
                getLog().warn("Failed to delete the previous incremental build digest");
            }
        }

        if (!getDigestCandidateFile().renameTo(getDigestFile())) {
            getLog().warn("Failed to accept the incremental build digest");
        }
    }

    private Log getLog() {
        return log;
    }

    private ArrayList<File> getDigestFiles() throws IOException {
        IncrementalVisitor visitor = new IncrementalVisitor();

        Files.walkFileTree(workingDirectory.toPath(), visitor);

        return visitor.getFiles();
    }

    static class IncrementalVisitor extends SimpleFileVisitor<Path> {
        static final Set<String> IGNORED_DIRS = new HashSet<>(Arrays.asList(
                "node_modules",
                "target"
        ));

        static final Set<String> DIGEST_EXTENSIONS = new HashSet<>(Arrays.asList(
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
                // config
                "json",
                "xml",
                "yaml",
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
        static final Set<String> DIGEST_FILES = new HashSet<>(Arrays.asList(
                ".parcelrc",
                ".babelrc",
                ".eslintrc",
                ".eslintignore",
                ".prettierrc",
                ".prettierignore",
                ".stylelintrc",
                ".stylelintignore",
                ".browserslistrc",
                ".nvmrc"
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
        return createFileDigest(digestFiles)
                + createToolsDigest()
                + createEnvironmentDigest();
    }

    private static String createFileDigest(ArrayList<File> digestFiles) {
        return digestFiles.parallelStream().map(file -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] byteArray = new byte[1024];
                    while (fis.read(byteArray) != -1) {
                        digest.update(byteArray);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                StringBuilder sb = new StringBuilder();
                for (byte b : digest.digest()) {
                    sb.append(String.format("%02x", b));
                }

                return file + " : " + sb + "\n";
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }).sorted().collect(Collectors.joining(""));
    }

    private static String createToolsDigest() {
        String[][] commands = {
                {"node", "--version"},
                {"yarn", "--version"},
                {"npm", "--version"}
        };

        return Arrays.stream(commands)
                .parallel()
                .map(IncrementalMojoHelper::createCommandDigest)
                .sorted()
                .collect(Collectors.joining());
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
        String[] variables = {
                "NODE_ENV",
                "BABEL_ENV",
                "OS",
                "OS_VERSION",
                "OS_ARCH",
                "OS_NAME",
                "OS_FAMILY"
        };

        StringBuilder sb = new StringBuilder();

        for (String variable : variables) {
            String value = System.getenv(variable);
            if (value != null) {
                value = value.replace("\n", " ");
            } else {
                value = "null";
            }

            sb.append("# ").append(variable).append(" = ").append(value).append("\n");
        }

        return sb.toString();
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
                getLog().info("File removed: " + prevFile);
            } else if (!prevHash.equals(currHash)) {
                getLog().info("File changed: " + prevFile);
            }
        }

        for (Map.Entry<String, String> entry : currDigestContents.entrySet()) {
            String currFile = entry.getKey();
            String prevHash = prevDigestContents.get(currFile);
            if (prevHash == null) {
                getLog().info("File added: " + currFile);
            }
        }
    }

    private Map<String, String> getDigestFilesMap(String digest) {
        return Arrays.stream(digest.split("\n"))
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
