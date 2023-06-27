package com.github.eirslett.maven.plugins.frontend.lib;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest {

  @Test
  public void testImplode() {
    final String separator = "Bar";
    final List<String> elements = new ArrayList<>();

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
            Utils.merge(singletonList("foo"), singletonList("bar"))
    );
  }

  @Test
  public void testPrepend() {
    assertEquals(
            Arrays.asList("foo", "bar"),
            Utils.prepend("foo", singletonList("bar"))
    );
  }
}
