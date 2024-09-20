package com.github.eirslett.maven.plugins.frontend.mojo;

import static com.github.eirslett.maven.plugins.frontend.mojo.YarnUtils.isYarnrcYamlFilePresent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;

@Mojo(name = "yarn", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class YarnMojo extends AbstractFrontendMojo {

    private static final String NPM_REGISTRY_URL = "npmRegistryURL";

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "", property = "frontend.yarn.arguments", required = false)
    private String arguments;

    @Parameter(property = "frontend.yarn.yarnInheritsProxyConfigFromMaven", required = false,
        defaultValue = "true")
    private boolean yarnInheritsProxyConfigFromMaven;

    /**
     * Registry override, passed as the registry option during npm install if set.
     */
    @Parameter(property = NPM_REGISTRY_URL, required = false, defaultValue = "")
    private String npmRegistryURL;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * The directory containing front end files that will be processed.
     * If this is set then files in the directory will be checked for
     * modifications before running yarn.
     */
    @Parameter(property = "srcdir", defaultValue = "src")
    private File srcdir;

    @Component
    private BuildContext buildContext;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.yarn", defaultValue = "${skip.yarn}")
    private boolean skip;

    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        if (shouldExecute()) {
            File packageJson = new File(this.workingDirectory, "package.json");
            if (this.buildContext == null || this.buildContext.hasDelta(packageJson)
                    || !this.buildContext.isIncremental()) {
                ProxyConfig proxyConfig = getProxyConfig();
                boolean isYarnBerry = isYarnrcYamlFilePresent(this.session, this.workingDirectory);
                factory.getYarnRunner(proxyConfig, getRegistryUrl(), isYarnBerry).execute(this.arguments,
                        this.environmentVariables);

                acceptIncrementalBuildDigest();
            } else {
                getLog().info("Skipping yarn install as package.json unchanged");
            }
        } else {
            getLog().info("Skipping yarn execution as no modified files in" + srcdir);
        }
    }

    private void acceptIncrementalBuildDigest() {
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

    private boolean shouldExecute() {
        if (arguments.equals("build")) {
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
        }

        return true;
    }

    private ArrayList<File> getDigestFiles() throws IOException {
        ArrayList<File> files = new ArrayList<>();

        Files.walkFileTree(workingDirectory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs)
            {
                if (file.endsWith("target")) {
                    return FileVisitResult.SKIP_SUBTREE;
                } else {
                    return FileVisitResult.CONTINUE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String filename = file.getFileName().toString();
                if (filename.endsWith(".js")) {
                    files.add(file.toFile());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    private static String createDigest(ArrayList<File> triggerfiles) {
        return triggerfiles.parallelStream().map(file -> {
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

                byte[] bytes = digest.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }

                return file + " : " + sb + "\n";
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }).sorted().collect(Collectors.joining(""));
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
        Map<String, String> prevDigestContents = getDigestContentMap(prevDigest);
        Map<String, String> currDigestContents = getDigestContentMap(currDigest);

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

    private Map<String, String> getDigestContentMap(String digest) {
        return Arrays.stream(digest.split("\n")).map(line -> line.split(" : "))
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

    private ProxyConfig getProxyConfig() {
        if (this.yarnInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(this.session, this.decrypter);
        } else {
            getLog().info("yarn not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.<ProxyConfig.Proxy>emptyList());
        }
    }

    private String getRegistryUrl() {
        // check to see if overridden via `-D`, otherwise fallback to pom value
        return System.getProperty(NPM_REGISTRY_URL, this.npmRegistryURL);
    }
}
