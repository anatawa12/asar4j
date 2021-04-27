package com.anatawa12.asar4j.url;

import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ConnectionTest {
    static {
        FactoryRegistrar.register();
    }

    @Test
    void onFileSystem() throws IOException {
        URL asarFile = new File(System.getProperty("com.anatawa12.asar4j.test-asar")).toURI().toURL();
        URL testText = AsarURLStreamHandlerFactory.createUrl(asarFile, "test.txt");
        // link
        URL testTextLink = AsarURLStreamHandlerFactory.createUrl(asarFile, "test.txt.1");

        assertArrayEquals("test text file\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(testText.openStream()));
        assertArrayEquals("test text file\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(testTextLink.openStream()));
    }

    @Test
    void onZipFile() throws IOException {
        URL zipFile = new File(System.getProperty("com.anatawa12.asar4j.test-zip")).toURI().toURL();
        URL asarFile = new URL("jar:" + zipFile + "!/test.asar");
        URL testText = AsarURLStreamHandlerFactory.createUrl(asarFile, "test.txt");
        // link
        URL testTextLink = AsarURLStreamHandlerFactory.createUrl(asarFile, "test.txt.1");

        assertArrayEquals("test text file\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(testText.openStream()));
        assertArrayEquals("test text file\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(testTextLink.openStream()));
    }
}
