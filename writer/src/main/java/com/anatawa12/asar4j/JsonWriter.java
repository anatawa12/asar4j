package com.anatawa12.asar4j;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public class JsonWriter extends FilterWriter {
    protected JsonWriter(Writer out) {
        super(out);
    }

    public void writeString(String value) throws IOException {
        write('"');
        int begin = 0;
        int end = 0;
        for (; end < value.length(); end++) {
            char c = value.charAt(end);
            if (c == '"') {
                write(value, begin, end - begin);
                begin = end + 1;
                write("\\\"");
            } else if (c == '\\') {
                write(value, begin, end - begin);
                begin = end + 1;
                write("\\\\");
            }
        }
        write(value, begin, end - begin);
        write('"');
    }

    public void writeKey(String key) throws IOException {
        writeString(key);
        write(':');
    }
}
