package com.github.eirslett.maven.plugins.frontend.lib;


import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class ArgumentsParserTest {

    @Test
    public void testNoArguments() {
        ArgumentsParser parser = new ArgumentsParser();

        assertEquals(0, parser.parse(null).size());
        assertEquals(0, parser.parse("null").size());
        assertEquals(0, parser.parse(String.valueOf("")).size());
    }

    @Test
    public void testMultipleArgumentsNoQuotes() {
        ArgumentsParser parser = new ArgumentsParser();

        assertArrayEquals(new Object[] { "foo" }, parser.parse("foo").toArray());
        assertArrayEquals(new Object[] { "foo", "bar" }, parser.parse("foo bar").toArray());
        assertArrayEquals(new Object[] { "foo", "bar", "foobar" }, parser.parse("foo bar foobar").toArray());
    }

    @Test
    public void testMultipleArgumentsWithQuotes() {
        ArgumentsParser parser = new ArgumentsParser();

        assertArrayEquals(new Object[] { "foo", "\"bar foobar\"" }, parser.parse("foo \"bar foobar\"").toArray());
        assertArrayEquals(new Object[] { "\"foo bar\"", "foobar" }, parser.parse("\"foo bar\" foobar").toArray());
        assertArrayEquals(new Object[] { "foo", "'bar foobar'" }, parser.parse("foo 'bar foobar'").toArray());
        assertArrayEquals(new Object[] { "'foo bar'", "foobar" }, parser.parse("'foo bar' foobar").toArray());
        // unclosed quotes
        assertArrayEquals(new Object[] { "foo", "\"bar foobar" }, parser.parse("foo \"bar foobar").toArray());
    }

    @Test
    public void testArgumentsWithMixedQuotes() {
        ArgumentsParser parser = new ArgumentsParser();

        assertArrayEquals(new Object[] { "foo", "\"bar 'foo bar'\"" }, parser.parse("foo \"bar 'foo bar'\"").toArray());
        assertArrayEquals(new Object[] { "foo", "\"bar 'foo\"", "'bar " }, parser.parse("foo \"bar 'foo\" 'bar ").toArray());
    }

    @Test
    public void repeatedArgumentsAreAccepted() {
        ArgumentsParser parser = new ArgumentsParser();

        assertArrayEquals(new Object[] { "echo", "echo" }, parser.parse("echo echo").toArray());
    }

    @Test
    public void testAdditionalArgumentsNoIntersection() {
        ArgumentsParser parser = new ArgumentsParser(Arrays.asList("foo", "bar"));

        assertArrayEquals(new Object[] { "foobar", "foo", "bar" }, parser.parse("foobar").toArray());
    }

    @Test
    public void testAdditionalArgumentsWithIntersection() {
        ArgumentsParser parser = new ArgumentsParser(Arrays.asList("foo", "foobar"));

        assertArrayEquals(new Object[] { "bar", "foobar", "foo" }, parser.parse("bar foobar").toArray());
    }
}
