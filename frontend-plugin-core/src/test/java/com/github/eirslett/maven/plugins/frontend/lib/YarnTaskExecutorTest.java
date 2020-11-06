package com.github.eirslett.maven.plugins.frontend.lib;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(YarnExecutor.class)
public class YarnTaskExecutorTest {

    @Test
    public void yarnArgumentsShouldBePassedToYarn() throws TaskRunnerException {
        InstallConfig installConfig = new DefaultInstallConfig(null, null, null, Platform.guess());
        YarnExecutorConfig yarnConfig = new InstallYarnExecutorConfig(installConfig);

        final List<String> caughtArugments = new ArrayList<>();
        YarnTaskExecutor executor = new YarnTaskExecutor(yarnConfig, "task", "location", Arrays.asList("--yarn-arg1", "--yarn-arg2")) {
            @Override
            YarnExecutor createExecutor(List<String> arguments, Map<String, String> environment) {
                caughtArugments.addAll(arguments);
                return PowerMock.createMock(YarnExecutor.class);
            }
        };

        executor.execute("build --arg", Collections.<String, String>emptyMap());

        assertArrayEquals(new String[] {"--yarn-arg1", "--yarn-arg2", "build", "--arg"}, caughtArugments.toArray(new String[caughtArugments.size()]));
    }
}
