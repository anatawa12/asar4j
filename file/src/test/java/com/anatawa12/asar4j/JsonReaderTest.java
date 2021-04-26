package com.anatawa12.asar4j;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonReaderTest {
    // region strings

    @Test
    void simpleEscape() throws IOException {
        assertEquals("\"\\/\b\f\n\r\t", reader("\"" +
                "\\\"" +
                "\\\\" +
                "\\/" +
                "\\b" +
                "\\f" +
                "\\n" +
                "\\r" +
                "\\t" +
                "\"").readString());
    }

    @Test
    void hexEscape() throws IOException {
        assertEquals("\u0000\uFFFF\uffff\u0123\u4567\u89ab\ucdef", reader("\"" +
                "\\u0000" +
                "\\uFFFF" +
                "\\uffff" +
                "\\u0123" +
                "\\u4567" +
                "\\u89ab" +
                "\\ucdef" +
                "\"").readString());
    }

    @Test
    void invalidEscape() {
        assertThrows(AsarException.class, () -> reader("\"\\a\"").readString());
    }

    @Test
    void readString() throws IOException {
        assertEquals("something", reader("\"something\"").readString());

        // space skipping
        JsonReader reader = reader("  \"first value\"\"second value\"   \"third value\"");
        assertEquals("first value", reader.readString());
        assertEquals("second value", reader.readString());
        assertEquals("third value", reader.readString());
    }

    //endregion

    //region numbers

    @Test
    void readNumber() throws IOException {
        assertEquals(10, reader("10").readNumber());
        assertEquals(10.0, reader("10.0").readNumber());
        assertEquals(10.0e2, reader("10.0e2").readNumber());
        assertEquals(10.0e2, reader("10.0e2").readNumber());
        assertEquals(-10, reader("-10").readNumber());

        // space skipping
        JsonReader reader = reader("  10-11 -12");
        assertEquals(10, reader.readNumber());
        assertEquals(-11, reader.readNumber());
        assertEquals(-12, reader.readNumber());
    }

    //endregion

    //region booleans

    @Test
    void readBoolean() throws IOException {
        assertTrue(reader("true").readBoolean());
        assertFalse(reader("false").readBoolean());

        // space skipping
        JsonReader reader = reader("  false   true  false");
        assertFalse(reader.readBoolean());
        assertTrue(reader.readBoolean());
        assertFalse(reader.readBoolean());
    }

    //endregion

    //region objects

    @Test
    void readObjectKey() throws IOException {
        assertEquals("theKey", reader("\"theKey\":").readKey());
        assertEquals("theKey", reader("\"theKey\"  :  ").readKey());

        // skip strings and trailing values
        JsonReader reader = reader("\"key1\" : 100.0  ,  \"key2\":101.0");
        assertEquals("key1", reader.readKey());
        assertEquals(100.0, reader.readNumber());
        reader.readOpt(',');
        assertEquals("key2", reader.readKey());
        assertEquals(101.0, reader.readNumber());
        reader.readOpt(',');
    }

    //endregion

    JsonReader reader(String body) {
        return new JsonReader(new StringReader(body));
    }
}
