package com.anatawa12.asar4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

final class JsonReader {
    final Reader reader;
    int cur = -2;

    JsonReader(Reader reader) {
        this.reader = reader;
    }

    void read(char c) throws IOException {
        char read;
        if ((read = read()) != c)
            throw new AsarException("Malformed json: expected '" + c + "' but was '" + read + '\"');
    }

    void readOpt(char c) throws IOException {
        if (cur() == c)
            cur = -2;
    }

    // get current char skipping whitespaces
    int cur() throws IOException {
        if (cur != -2) return cur;
        while (true) {
            cur = reader.read();
            if (cur == '\u0020') continue;
            if (cur == '\r') continue;
            if (cur == '\n') continue;
            if (cur == '\u0009') continue;
            return cur;
        }
    }

    private char read() throws IOException {
        if (cur() == -1)
            throw new EOFException();
        char c = (char) cur;
        cur = -2;
        return c;
    }

    private char readChar() throws IOException {
        if (cur == -2) cur = reader.read();
        if (cur == -1) throw new EOFException();
        char c = (char) cur;
        cur = -2;
        return c;
    }

    private int readCharOpt() throws IOException {
        if (cur == -2) cur = reader.read();
        if (cur == -1) return -1;
        char c = (char) cur;
        cur = -2;
        return c;
    }

    public String readKey() throws IOException {
        String key = readString();
        read(':');
        return key;
    }

    public String readString() throws IOException {
        StringBuilder builder = new StringBuilder();
        read('"');
        char c;
        while (true) {
            c = readChar();
            if (c == '"') break;
            if (c != '\\') {
                builder.append(c);
            } else {
                switch (c = readChar()) {
                    case '"':
                        builder.append('"');
                        break;
                    case '\\':
                        builder.append('\\');
                        break;
                    case '/':
                        builder.append('/');
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        builder.append(fourHex());
                        break;
                    default:
                        throw new AsarException("Illegal escape sequence:" + c);
                }
            }
        }
        return builder.toString();
    }

    private char fourHex() throws IOException {
        return (char) (hex() << 12
                | hex() << 8
                | hex() << 4
                | hex());
    }

    private int hex() throws IOException {
        char c = readChar();
        if ('0' <= c && c <= '9') return c - '0';
        if ('A' <= c && c <= 'F') return c - 'A' + 10;
        if ('a' <= c && c <= 'f') return c - 'a' + 10;
        throw new AsarException("Illegal hex:" + c);
    }

    private String readToken() throws IOException {
        if (cur() == '"') throw new AsarException("token expected but was string");
        StringBuilder builder = new StringBuilder();
        int c = read();
        if (!('0' <= c && c <= '9'
                || 'a' <= c && c <= 'z'
                || 'A' <= c && c <= 'Z'
                || c == '.'
                || c == '-'))
            throw new AsarException("Unexpected Character: '" + (char) c + '\'');
        builder.append((char) c);

        loop:
        while (true) {
            c = readCharOpt();
            assert cur < 0;
            switch (c) {
                case -1:
                case ',':
                case ':':
                case '{':
                case '}':
                case '[':
                case ']':
                case '-':
                case '"':
                    cur = c;
                    break loop;
                case ' ':
                case '\r':
                case '\n':
                case '\u0009':
                    cur = -2;
                    break loop;
            }
            if (!('0' <= c && c <= '9'
                    || 'a' <= c && c <= 'z'
                    || 'A' <= c && c <= 'Z'
                    || c == '.'))
                throw new AsarException("Unexpected Character: '" + (char) c + '\'');
            builder.append((char) c);
        }
        return builder.toString();
    }

    public boolean readBoolean() throws IOException {
        String token = readToken();
        switch (token) {
            case "true":
                return true;
            case "false":
                return false;
            default:
                throw new AsarException("boolean expected but was " + token);
        }
    }

    public double readNumber() throws IOException {
        try {
            return Double.parseDouble(readToken());
        } catch (NumberFormatException e) {
            throw new AsarException("invalid number", e);
        }
    }

    public void skipValue() throws IOException {
        switch (cur()) {
            case '"':
                readString();
                break;
            case '{':
                read();
                skipObjectBodyRemain();
                read('}');
                break;
            case '[':
                read();
                while (cur() != ']') {
                    skipValue();
                    readOpt(',');
                }
        }
    }

    public void skipObjectBodyRemain() throws IOException {
        while (cur() != '}') {
            readKey();
            skipValue();
            readOpt(',');
        }
    }
}
