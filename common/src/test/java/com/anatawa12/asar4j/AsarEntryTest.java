package com.anatawa12.asar4j;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsarEntryTest {
    @Test
    void isNormalized() {
        assertTrue(AsarEntry.isNormalized(""));
        assertTrue(AsarEntry.isNormalized("/.txt"));
        assertTrue(AsarEntry.isNormalized("/file.txt"));
        assertTrue(AsarEntry.isNormalized("/directory/file.txt"));

        for (NormalizeCheck normalizeCheck : normalizeChecks) {
            assertFalse(AsarEntry.isNormalized(normalizeCheck.original));
        }
    }

    @Test
    void components() {
        assertEquals(ImmutableList.of(), AsarEntry.components(""));

        assertEquals(ImmutableList.of(".txt"), AsarEntry.components("/.txt"));
        assertEquals(ImmutableList.of("file.txt"), AsarEntry.components("/file.txt"));
        assertEquals(ImmutableList.of("directory", "file.txt"), AsarEntry.components("/directory/file.txt"));

        assertEquals(ImmutableList.of(".", "file.txt"), AsarEntry.components("/./file.txt"));
        assertEquals(ImmutableList.of(".", "file.txt"), AsarEntry.components("./file.txt"));
        assertEquals(ImmutableList.of("..", "file.txt"), AsarEntry.components("../file.txt"));

        assertEquals(ImmutableList.of("directory", ".", "file.txt"), AsarEntry.components("directory/./file.txt"));
        assertEquals(ImmutableList.of("directory", "..", "file.txt"), AsarEntry.components("directory/../file.txt"));
        assertEquals(ImmutableList.of("directory", ".", "file.txt"), AsarEntry.components("/directory/./file.txt"));
        assertEquals(ImmutableList.of("directory", "..", "file.txt"), AsarEntry.components("/directory/../file.txt"));

        for (NormalizeCheck normalizeCheck : normalizeChecks) {
            if (normalizeCheck.normalized == null) continue;
            assertEquals(AsarEntry.components(normalizeCheck.normalized),
                    AsarEntry.components(normalizeCheck.original));
        }
    }

    @Test
    void normalizeName() {
        // should be same for literals.
        assertSame("", AsarEntry.normalizeName(""));
        assertSame("/.txt", AsarEntry.normalizeName("/.txt"));
        assertSame("/file.txt", AsarEntry.normalizeName("/file.txt"));
        assertSame("/directory/file.txt", AsarEntry.normalizeName("/directory/file.txt"));

        for (NormalizeCheck normalizeCheck : normalizeChecks) {
            if (normalizeCheck.normalized == null) {
                assertThrows(IllegalArgumentException.class, () -> AsarEntry.normalizeName(normalizeCheck.original));
            } else {
                assertEquals(normalizeCheck.normalized, AsarEntry.normalizeName(normalizeCheck.original));
            }
        }
    }

    private static final NormalizeCheck[] normalizeChecks = new NormalizeCheck[]{
            new NormalizeCheck("/", ""),
            new NormalizeCheck("file.txt", "/file.txt"),
            new NormalizeCheck("directory/file.txt", "/directory/file.txt"),
            new NormalizeCheck("directory//file.txt", "/directory/file.txt"),
            new NormalizeCheck("/directory//file.txt", "/directory/file.txt"),
            // invalids
            new NormalizeCheck("/./file.txt", null),
            new NormalizeCheck("/../file.txt", null),
            new NormalizeCheck("/directory/..", null),
            new NormalizeCheck("/directory/.", null),
    };

    private static class NormalizeCheck {
        final String original;
        // null -> invalid
        final String normalized;

        public NormalizeCheck(String original, String normalized) {
            this.original = original;
            this.normalized = normalized;
        }
    }
}
