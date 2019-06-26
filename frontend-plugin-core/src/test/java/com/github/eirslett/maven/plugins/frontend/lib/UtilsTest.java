package com.github.eirslett.maven.plugins.frontend.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

  @Test
  public void testImplode() {
    final String separator = "Bar";
    final List<String> elements = new ArrayList<String>();

    assertEquals("", Utils.implode(separator, elements));

    elements.add("foo");
    elements.add("bar");
    assertEquals("foo bar", Utils.implode(separator, elements));
  }

  @Test
  public void testIsRelative() {
    assertTrue(Utils.isRelative("foo/bar"));

    assertFalse(Utils.isRelative("/foo/bar"));
    assertFalse(Utils.isRelative("file:foo/bar"));
    assertFalse(Utils.isRelative("C:\\SYSTEM"));
  }

  @Test
  public void testMerge() {
    assertEquals(
            Arrays.asList("foo", "bar"),
            Utils.merge(Arrays.asList("foo"), Arrays.asList("bar"))
    );
  }

  @Test
  public void testPrepend() {
    assertEquals(
            Arrays.asList("foo", "bar"),
            Utils.prepend("foo", Arrays.asList("bar"))
    );
  }
}
