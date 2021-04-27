package com.anatawa12.asar4j.url;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlUtilTest {
    @Test
    void decodeNonEscaped() {
        // simple
        assertSame("some text", UrlUtil.decodeURL("some text"));
        // utf8
        assertSame("\u65e5\u672c\u8a9e", UrlUtil.decodeURL("\u65e5\u672c\u8a9e"));
        // surrogate
        assertSame("\ud842\udfb7", UrlUtil.decodeURL("\ud842\udfb7"));
    }

    @Test
    void decodeEscaped() {
        // range test

        // 1 bytes
        assertEquals("\u0000", UrlUtil.decodeURL("%00"));
        assertEquals("\u007F", UrlUtil.decodeURL("%7F"));

        // 2 bytes
        assertInvalid("%C1%BF"); // U+007f in 2 bytes
        assertEquals("\u0080", UrlUtil.decodeURL("%C2%80"));
        assertEquals("\u07FF", UrlUtil.decodeURL("%DF%BF"));

        // 3 bytes
        assertInvalid("%E0%9F%BF"); // U+07FF in 3 bytes
        assertEquals("\u0800", UrlUtil.decodeURL("%E0%A0%80"));
        assertEquals("\uFFFF", UrlUtil.decodeURL("%EF%BF%BF"));

        // 4 bytes
        assertInvalid("%F0%8F%BF%BF"); // U+FFFF in 4 bytes
        assertEquals(surrogate(0x010000), UrlUtil.decodeURL("%F0%90%80%80"));
        assertEquals(surrogate(0x10FFFF), UrlUtil.decodeURL("%F4%8F%BF%BF"));
        assertInvalid("%F4%90%80%80"); // U+110000 in 4 bytes

        // 5 bytes
        assertInvalid("%F8%8F%BF%BF%BF");
        // 6 bytes
        assertInvalid("%FC%8F%BF%BF%BF%BF");
        // FE and FF (duplicates with BOM)
        assertInvalid("%FE%8F%BF%BF%BF%BF%BF");
        assertInvalid("%FF%8F%BF%BF%BF%BF%BF%BF");

        // simple
        assertEquals("some text", UrlUtil.decodeURL("some%20text"));
        // utf8
        assertEquals("\u65e5\u672c\u8a9e text", UrlUtil.decodeURL("%e6%97%a5%e6%9c%ac%e8%aa%9e text"));
        // surrogate
        assertEquals("some \ud842\udfb7 text", UrlUtil.decodeURL("some %f0%a0%ae%b7 text"));
    }

    private void assertInvalid(String s) {
        assertThrows(IllegalArgumentException.class,
                () -> UrlUtil.decodeURL(s));
    }

    private String surrogate(int c) {
        return new String(Character.toChars(c));
    }
}
