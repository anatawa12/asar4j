package com.anatawa12.asar4j.url;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URLTest {
    static {
        URL.setURLStreamHandlerFactory(AsarURLStreamHandlerFactory.INSTANCE);
    }

    @Test
    void general() throws Throwable {
        assertEquals("asar:file:/a.asar!/", new URL("asar:file:/a.asar!/").toString());
        assertEquals("asar:file:/a.asar!/test", new URL("asar:file:/a.asar!/test").toString());
        assertEquals("asar:file:/a.asar!/test", new URL("asar:file:/a.asar!//test").toString());
        assertEquals("asar:file:/a.asar!/test/", new URL("asar:file:/a.asar!//test/").toString());
        // hash
        assertEquals("asar:file:/a.asar!/test/#test", new URL("asar:file:/a.asar!//test/#test").toString());
    }

    @Test
    void contextual() throws Throwable {
        URL root = new URL("asar:file:/a.asar!/");
        URL test = new URL("asar:file:/a.asar!/test");
        URL testDir = new URL("asar:file:/a.asar!/test/");

        assertEquals("asar:file:/a.asar!/test1", new URL(root, "test1").toString());
        assertEquals("asar:file:/a.asar!/test1", new URL(test, "test1").toString());
        assertEquals("asar:file:/a.asar!/test/test1", new URL(testDir, "test1").toString());
        assertEquals("asar:file:/a.asar!/test1", new URL(testDir, "/test1").toString());
        // hash
        assertEquals("asar:file:/a.asar!/test#test", new URL(test, "#test").toString());
        assertEquals("asar:file:/a.asar!/test/#test", new URL(testDir, "#test").toString());
    }

    @Test
    void creating() throws Throwable {
        assertEquals("asar:file:/a.asar!/test1",
                AsarURLStreamHandlerFactory.createUrl(new URL("file:/a.asar"), "test1").toString());
        assertEquals("asar:file:/a.asar!/test1%21",
                AsarURLStreamHandlerFactory.createUrl(new URL("file:/a.asar"), "test1!").toString());

        // inzip
        assertEquals("asar:jar:file:/test.zip%21/test.asar!/test1",
                AsarURLStreamHandlerFactory.createUrl(new URL("jar:file:/test.zip!/test.asar"), "test1").toString());
        assertEquals("asar:jar:file:/test.zip%21/test.asar!/test1/",
                AsarURLStreamHandlerFactory.createUrl(new URL("jar:file:/test.zip!/test.asar"), "test1/").toString());
    }

    @Test
    void sameFile() throws Throwable {
        URL root = new URL("asar:file:/a.asar!/");
        URL test = new URL("asar:file:/a.asar!/test");
        URL testDir = new URL("asar:file:/a.asar!/test/");

        assertFalse(root.sameFile(test));
        assertTrue(test.sameFile(testDir));
        assertTrue(testDir.sameFile(test));
    }
}
