package com.anatawa12.asar4j;

import com.google.common.io.ByteStreams;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Utils {
    static SeekableByteChannel getFileAsChannel(String name) throws IOException {
        return new SeekableInMemoryByteChannel(getFileAsBytes(name));
    }

    static String getFileAsString(String name) throws IOException {
        return new String(getFileAsBytes(name), StandardCharsets.UTF_8);
    }

    static byte[] getFileAsBytes(String name) throws IOException {
        return ByteStreams.toByteArray(Objects.requireNonNull(
                Utils.class.getClassLoader().getResourceAsStream(name)));
    }
}
