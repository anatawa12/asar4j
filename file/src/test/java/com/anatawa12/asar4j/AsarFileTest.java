package com.anatawa12.asar4j;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsarFileTest {
    @Test
    void allTest() throws IOException {
        AsarFile file = new AsarFile(Utils.getFileAsChannel("test1.asar"));

        assertNotNull(file.getEntry("test.txt"));
        assertArrayEquals("test text file\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(file.getInputStream(file.getEntry("test.txt"))));
        assertArrayEquals("test text file\n".getBytes(StandardCharsets.UTF_8),
                ByteStreams.toByteArray(file.getInputStream(file.getEntry("test.txt.1"))));
    }

    @Test
    void readEntry() throws IOException {
        AsarEntry root = AsarFile.readEntry(new JsonReader(new StringReader(
                Utils.getFileAsString("header1.json"))), "", null);
        assertTrue(root instanceof AsarDirectoryEntry);

        assertEquals("/files", root.getChild("files").getName());
        assertEquals(AsarEntryType.FILE, root.getChild("files").getType());
        assertEquals(100, root.getChild("files").getSize());
        assertEquals(100, root.getChild("files").offset);

        {
            AsarEntry directory = root.getChild("directory");
            assertEquals("/directory", root.getChild("directory").getName());
            assertEquals(AsarEntryType.DIRECTORY, root.getChild("directory").getType());
            assertEquals(1, root.getChild("directory").getChildren().size());

            assertEquals("/directory/in-dir-link", directory.getChild("in-dir-link").getName());
            assertEquals(AsarEntryType.LINK, directory.getChild("in-dir-link").getType());
            assertEquals("../files", directory.getChild("in-dir-link").getLinkTarget());
        }

        assertEquals("/link", root.getChild("link").getName());
        assertEquals(AsarEntryType.LINK, root.getChild("link").getType());
        assertEquals("files", root.getChild("link").getLinkTarget());
    }

    @Test
    void getEntry() throws IOException {
        AsarFile file = new AsarFile(Utils.getFileAsChannel("test1.asar"));

        // no exception check
        file.getEntry("some");
        file.getEntry("some/some2");
        file.getEntry("/some/some2");
        file.getEntry("/some/some2.");
        file.getEntry("/some/some2.txt");
        file.getEntry("/some/.something");
        file.getEntry("/some/..something");
        file.getEntry("/some/some2..txt");
        assertThrows(IllegalArgumentException.class, () -> file.getEntry("some/.."));
        assertThrows(IllegalArgumentException.class, () -> file.getEntry("/some/.."));
        assertThrows(IllegalArgumentException.class, () -> file.getEntry("."));
        assertThrows(IllegalArgumentException.class, () -> file.getEntry("/some/./second"));
        assertThrows(IllegalArgumentException.class, () -> file.getEntry("/some/../second"));
    }

    @Test
    void resolveEntry() {
        // pre-test: building tree by hand;
        AsarEntry file = new AsarEntry("/file");
        AsarLinkEntry inDirLink = new AsarLinkEntry("/directory/in-dir-link", "../file");
        AsarDirectoryEntry directory = new AsarDirectoryEntry("directory",
                ImmutableMap.<String, AsarEntry>builder()
                        .put("in-dir-link", inDirLink)
                        .build());
        AsarLinkEntry link = new AsarLinkEntry("/link", "file");
        AsarDirectoryEntry root = new AsarDirectoryEntry("",
                ImmutableMap.<String, AsarEntry>builder()
                        .put("file", file)
                        .put("directory", directory)
                        .put("link", link)
                        .build());

        // test: resolveEntry

        assertSame(root, AsarFile.resolveEntry(listOf(), false, root));
        assertSame(root, AsarFile.resolveEntry(listOf(), true, root));

        assertSame(file, AsarFile.resolveEntry(listOf("file"), false, root));
        assertSame(file, AsarFile.resolveEntry(listOf("file"), true, root));

        assertSame(link, AsarFile.resolveEntry(listOf("link"), false, root));
        assertSame(file, AsarFile.resolveEntry(listOf("link"), true, root));

        assertSame(directory, AsarFile.resolveEntry(listOf("directory"), false, root));
        assertSame(directory, AsarFile.resolveEntry(listOf("directory"), true, root));

        assertSame(inDirLink, AsarFile.resolveEntry(listOf("directory", "in-dir-link"), false, root));
        assertSame(file, AsarFile.resolveEntry(listOf("directory", "in-dir-link"), true, root));
    }

    @SafeVarargs
    private final <E> List<E> listOf(E... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

}
