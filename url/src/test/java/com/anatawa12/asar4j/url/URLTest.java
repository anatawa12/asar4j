package com.anatawa12.asar4j.url;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLTest {
    static {
        URL.setURLStreamHandlerFactory(AsarURLStreamHandlerFactory.INSTANCE);
    }

    @Test
    void general() throws Throwable {
        assertEquals("asar:file:///a.asar!/", new URL("asar:file:///a.asar!/").toString());
        assertEquals("asar:file:///a.asar!/test", new URL("asar:file:///a.asar!/test").toString());
        assertEquals("asar:file:///a.asar!/test", new URL("asar:file:///a.asar!//test").toString());
        assertEquals("asar:file:///a.asar!/test/", new URL("asar:file:///a.asar!//test/").toString());
        // hash
        assertEquals("asar:file:///a.asar!/test/#test", new URL("asar:file:///a.asar!//test/#test").toString());
    }

    @Test
    void contextual() throws Throwable {
        URL root = new URL("asar:file:///a.asar!/");
        URL test = new URL("asar:file:///a.asar!/test");
        URL testDir = new URL("asar:file:///a.asar!/test/");

        assertEquals("asar:file:///a.asar!/test1", new URL(root, "test1").toString());
        assertEquals("asar:file:///a.asar!/test1", new URL(test, "test1").toString());
        assertEquals("asar:file:///a.asar!/test/test1", new URL(testDir, "test1").toString());
        // hash
        assertEquals("asar:file:///a.asar!/test#test", new URL(test, "#test").toString());
        assertEquals("asar:file:///a.asar!/test/#test", new URL(testDir, "#test").toString());
    }
}
