package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public interface NodeExecutorConfig {
  File getNodePath();
  File getNpmPath();
  File getPnpmPath();
  File getPnpmCjsPath();

  File getNpxPath();
  File getInstallDirectory();
  File getWorkingDirectory();
  Platform getPlatform();
}

final class InstallNodeExecutorConfig implements NodeExecutorConfig {

  private static final String NODE_WINDOWS = NodeInstaller.INSTALL_PATH.replaceAll("/", "\\\\") + "\\node.exe";
  private static final String NODE_DEFAULT = NodeInstaller.INSTALL_PATH + "/node";
  private static final String NPM = NodeInstaller.INSTALL_PATH + "/node_modules/npm/bin/npm-cli.js";
  private static final String PNPM = NodeInstaller.INSTALL_PATH + "/node_modules/pnpm/bin/pnpm.js";
  private static final String PNPM_CJS = NodeInstaller.INSTALL_PATH + "/node_modules/pnpm/bin/pnpm.cjs";
  private static final String NPX = NodeInstaller.INSTALL_PATH + "/node_modules/npm/bin/npx-cli.js";

  private final InstallConfig installConfig;

  public InstallNodeExecutorConfig(InstallConfig installConfig) {
    this.installConfig = installConfig;
  }

  @Override
  public File getNodePath() {
    String nodeExecutable = getPlatform().isWindows() ? NODE_WINDOWS : NODE_DEFAULT;
    return new File(installConfig.getInstallDirectory() + nodeExecutable);
  }

  @Override
  public File getNpmPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(NPM));
  }


  @Override
  public File getPnpmPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(PNPM));
  }

  @Override
  public File getPnpmCjsPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(PNPM_CJS));
  }

  @Override
  public File getNpxPath() {
    return new File(installConfig.getInstallDirectory() + Utils.normalize(NPX));
  }

  @Override
  public File getInstallDirectory() {
    return installConfig.getInstallDirectory();
  }

  @Override
  public File getWorkingDirectory() {
    return installConfig.getWorkingDirectory();
  }

  @Override
  public Platform getPlatform() {
    return installConfig.getPlatform();
  }
}

final class NodeSystemExecuterConfig implements NodeExecutorConfig {
  private final String nodeExecutable;
  private final String npmExecutable;


  private final String npm;
  private final String pnpm;
  private final String pnpmCjs;
  private final String npx;

  private final InstallConfig installConfig;

  public NodeSystemExecuterConfig(InstallConfig installConfig) {
    this.installConfig = installConfig;
    String nodeBinaryName = getPlatform().isWindows() ? "node.exe" : "node";
    String environmentSeparator = getPlatform().isWindows() ? ";" : ":";
    String npmCliName = getPlatform().isWindows() ? "npm.cmd" : "npm";

    nodeExecutable = getPathOfExecutable(nodeBinaryName, environmentSeparator);
    npmExecutable = getPathOfExecutable(npmCliName, environmentSeparator);


    String systemLibDir = findGloballyInstalledPackages();


    npm = systemLibDir + "/npm/bin/npm-cli.js";
    pnpm = systemLibDir + "/pnpm/bin/pnpm.js";
    pnpmCjs = systemLibDir + "/pnpm/bin/pnpm.cjs";
    npx = systemLibDir + "/npm/bin/npx-cli.js";
  }

  private String findGloballyInstalledPackages() {
    String systemLibDir;
    try {
      Runtime runtime = Runtime.getRuntime();
      Process process = runtime.exec(new String[]{npmExecutable, "root", "-g"});
      try (BufferedReader br = new BufferedReader(
              new InputStreamReader(process.getInputStream(),
              StandardCharsets.UTF_8))) {
        systemLibDir = br.readLine().trim();
      }
      process.destroy();
    } catch (IOException | NullPointerException e) {
      throw new IllegalStateException("NPM cannot be executed!", e);
    }
    return systemLibDir;
  }

  private String getPathOfExecutable(String binaryName, String environmentSeparator) {
    return Arrays.stream(System.getenv("PATH").split(environmentSeparator))
            .map(File::new)
            .filter(File::isDirectory)
            .map(file -> new File(file.getAbsolutePath() + File.separator + binaryName))
            .filter(File::exists)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(binaryName + " was not found on the system"))
            .getAbsolutePath();
  }

  @Override
  public File getNodePath() {
    return new File(nodeExecutable);
  }

  @Override
  public File getNpmPath() {
    return new File(Utils.normalize(npm));
  }


  @Override
  public File getPnpmPath() {
    return new File(Utils.normalize(pnpm));
  }

  @Override
  public File getPnpmCjsPath() {
    return new File(Utils.normalize(pnpmCjs));
  }

  @Override
  public File getNpxPath() {
    return new File(Utils.normalize(npx));
  }

  @Override
  public File getInstallDirectory() {
    return new File(nodeExecutable);
  }

  @Override
  public File getWorkingDirectory() {
    return installConfig.getWorkingDirectory();
  }

  @Override
  public Platform getPlatform() {
    return installConfig.getPlatform();
  }
}
