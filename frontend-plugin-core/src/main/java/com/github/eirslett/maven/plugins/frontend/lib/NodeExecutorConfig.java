package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.concurrent.Callable;

public interface NodeExecutorConfig {
  File getNodePath();

  /**
   * Lazy because NPM needs to be unextracted before this can be calculated.
   *
   */
  Callable<File> getNpmPath();
  File getWorkingDirectory();
  Platform getPlatform();
}

final class InstallNodeExecutorConfig implements NodeExecutorConfig {

  private static final String NODE_WINDOWS = "\\node\\node.exe";
  private static final String NODE_DEFAULT = "/node/node";
  public static final String NPM_START = "/node/node_modules/";
  public static final String BIN_NPM_CLI_JS = "/bin/npm-cli.js";
  public static final String NPM_FOLDER_REGEX = "npm(-\\d.*)?";

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
  public Callable<File> getNpmPath() {
    return new Callable<File>() {
      @Override
      public File call() throws Exception {
        String npmStartNormalized = Utils.normalize(installConfig.getInstallDirectory() + NPM_START);

        File npmStartFolder = new File(npmStartNormalized);
        boolean exists = npmStartFolder.exists();

        if (!exists) {
          throw new InstallationException("NPM folder " + npmStartNormalized + " does not exist");
        }

        String[] npmFolder = npmStartFolder.list(new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.matches(NPM_FOLDER_REGEX);
          }
        });

        switch (npmFolder.length) {
          case 0:
            throw new InstallationException("Couldn't find NPM folder in " + npmStartFolder);
          case 1:
            return new File(installConfig.getInstallDirectory() + Utils.normalize(NPM_START + npmFolder[0] + BIN_NPM_CLI_JS));
          default:
            throw new InstallationException("Multiple matches for NPM folder: " + Arrays.toString(npmFolder));
        }
      }
    };
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