package com.github.eirslett.maven.plugins.frontend.lib;

import com.diffblue.deeptestutils.Reflector;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NPMInstaller;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NPMInstallerTestCase {

  @Rule public ExpectedException thrown = ExpectedException.none();

  /* testedClasses: NPMInstaller */
  /*
   * Test generated by Diffblue Deeptest.
   * This test case covers:
   *  - conditional line 64 branch to line 71
   */

  @Test
  public void npmProvidedOutputFalse() throws NoSuchMethodException, IllegalAccessException,
                                              InstallationException, InvocationTargetException {

    // Arrange
    NPMInstaller objectUnderTest = ((NPMInstaller)Reflector.getInstance(
        "com.github.eirslett.maven.plugins.frontend.lib.NPMInstaller"));
    Reflector.setField(objectUnderTest, "logger", null);
    Reflector.setField(objectUnderTest, "nodeVersion", null);
    Reflector.setField(objectUnderTest, "password", null);
    Reflector.setField(objectUnderTest, "config", null);
    Reflector.setField(objectUnderTest, "fileDownloader", null);
    Reflector.setField(objectUnderTest, "npmVersion", "\"@00h");
    Reflector.setField(objectUnderTest, "npmDownloadRoot", null);
    Reflector.setField(objectUnderTest, "archiveExtractor", null);
    Reflector.setField(objectUnderTest, "userName", null);

    // Act
    Class<?> classUnderTest =
        Reflector.forName("com.github.eirslett.maven.plugins.frontend.lib.NPMInstaller");
    Method methodUnderTest = classUnderTest.getDeclaredMethod("npmProvided");
    methodUnderTest.setAccessible(true);
    boolean retval = (boolean)methodUnderTest.invoke(objectUnderTest);

    // Assert result
    Assert.assertEquals(false, retval);
  }
}
