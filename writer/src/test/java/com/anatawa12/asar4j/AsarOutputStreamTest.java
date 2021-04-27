package com.anatawa12.asar4j;

import com.google.common.io.ByteStreams;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AsarOutputStreamTest {
    @Test
    void readAndWrite() throws IOException {
        AsarFile file = createFile((stream) -> {
            stream.putNextEntry(new AsarEntry("/test.txt"));
            stream.write("/test.txt file body\n".getBytes(StandardCharsets.UTF_8));
            stream.putNextEntry(new AsarEntry("/directory/test.txt"));
            stream.write("/directory/test.txt file body\n".getBytes(StandardCharsets.UTF_8));
        });

        assertArrayEquals("/test.txt file body\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(file.getInputStream(file.getEntry("/test.txt"))));
        assertArrayEquals("/directory/test.txt file body\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(file.getInputStream(file.getEntry("/directory/test.txt"))));
        assertEquals(AsarEntryType.DIRECTORY, file.getEntry("/directory").getType());
    }

    AsarFile createFile(AsarGenerator generator) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (AsarOutputStream stream = new AsarOutputStream(byteArrayOutputStream)) {
            generator.generate(stream);
        }
        return new AsarFile(new SeekableInMemoryByteChannel(byteArrayOutputStream.toByteArray()));
    }

    interface AsarGenerator {
        void generate(AsarOutputStream stream) throws IOException;
    }
}
