package com.anatawa12.asar4j.url;

class UrlUtil {
    private UrlUtil() {
    }

    static String decodeURL(String s) {
        int len = s.length();
        if (len == 0 || s.indexOf('%') < 0) return s;

        StringBuilder builder = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c != '%') {
                builder.append(c);
                continue;
            }

            // parse %xx%xx... sequence
            if (len - i < 3) throw new IllegalArgumentException("malformed url");
            byte b = twoHex(s, i);
            i += 3;
            int bytes;
            if ((b & 0x80) == 0x00) bytes = 0;
            else if ((b & 0xE0) == 0xC0) bytes = 1;
            else if ((b & 0xF0) == 0xE0) bytes = 2;
            else if ((b & 0xF8) == 0xF0) bytes = 3;
            else throw new IllegalArgumentException("malformed url");
            int data = b & firstMasks[bytes];
            for (int rep = 0; rep < bytes; rep++) {
                if (len - i < 3) throw new IllegalArgumentException("malformed url");
                if (s.charAt(i) != '%') throw new IllegalArgumentException("malformed url");
                b = twoHex(s, i);
                i += 3;
                if ((b & 0xC0) != 0x80)
                    throw new IllegalArgumentException("malformed url");
                data <<= 6;
                data |= b & 0x3F;
            }

            // verify utf8
            if (data < minValue[bytes]) throw new IllegalArgumentException("malformed url");
            if (data > 0x10FFFF) throw new IllegalArgumentException("malformed url");

            // append
            if (data <= 0xFFFF) {
                builder.append((char) data);
            } else {
                data -= 0x10000;
                builder.append((char) ((data >>> 10) + 0xD800));
                builder.append((char) ((data & 0x3FF) + 0xDC00));
            }
            // to un-skip
            i--;
        }

        return builder.toString();
    }

    static String encodeURL(String s) {
        int len = s.length();
        if (!needEscape(s)) return s;

        StringBuilder builder = new StringBuilder(len + 3);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == '!') builder.append("%21");
            else if (c == '%') builder.append("%25");
            else builder.append(c);
        }
        return builder.toString();
    }

    private static boolean needEscape(String s) {
        for (int i = 0; i < s.length(); i++) {
            if ("%!?#".contains(s)) return true;
        }
        return false;
    }

    private static final byte[] firstMasks = new byte[]{0x7F, 0x1F, 0x0F, 0x07};
    private static final int[] minValue = new int[]{0x000000, 0x000080, 0x000800, 0x010000};

    private static byte twoHex(String s, int i) {
        return (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
    }
}
