package com.anatawa12.asar4j.url;

import com.anatawa12.asar4j.AsarEntry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

class AsarURLHandler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new AsarURLConnection(u);
    }

    @Override
    protected boolean sameFile(URL u1, URL u2) {
        if (!u1.getProtocol().equals("jar")) return false;
        if (!u2.getProtocol().equals("jar")) return false;

        int sep1 = u1.getFile().indexOf("!/");
        int sep2 = u1.getFile().indexOf("!/");
        if (sep1 == -1 || sep2 == -1)
            return super.sameFile(u1, u2);

        try {
            if (!AsarEntry.normalizeName(UrlUtil.decodeURL(u1.getFile().substring(sep1 + 2)))
                    .equals(AsarEntry.normalizeName(UrlUtil.decodeURL(u2.getFile().substring(sep2 + 2)))))
                return false;
        } catch (IllegalArgumentException ignored) {
            return super.sameFile(u1, u2);
        }

        URL asarFileURL1;
        URL asarFileURL2;
        try {
            asarFileURL1 = new URL(UrlUtil.decodeURL(u1.getFile().substring(0, sep1)));
            asarFileURL2 = new URL(UrlUtil.decodeURL(u2.getFile().substring(0, sep2)));
        } catch (MalformedURLException e) {
            return super.sameFile(u1, u2);
        }

        return super.sameFile(asarFileURL1, asarFileURL2);
    }

    @Override
    protected void parseURL(URL url, String spec, int start, int limit) {
        String file = null;
        String ref = null;
        // first figure out if there is an anchor
        int refPos = spec.indexOf('#', limit);
        boolean refOnly = refPos == start;
        if (refPos > -1) {
            ref = spec.substring(refPos + 1);
            if (refOnly) {
                file = url.getFile();
            }
        }
        // then figure out if the spec is
        // 1. asar:....
        // 2. url + foo/bar/baz.ext)
        // 3. url + #foo
        boolean fullSpec = false;
        if (spec.length() >= 4) {
            fullSpec = spec.substring(0, 5).equalsIgnoreCase("asar:");
        }
        spec = spec.substring(start, limit);

        if (fullSpec) {
            file = spec;
        } else if (!refOnly) {
            file = parseRelativeSpec(url, spec);
        }
        file = parseFullSpec(file);
        setURL(url, "asar", "", -1,
                null, null, file, null, ref);
    }

    private String parseFullSpec(String spec) {
        int index = spec.indexOf("!/");
        if (index == -1)
            throw new IllegalArgumentException("no !/ in spec");
        String asarURLPart = spec.substring(0, index);
        String inAsar;
        try {
            inAsar = AsarEntry.normalizeName(UrlUtil.decodeURL(spec.substring(index + 1)));
            new URL(UrlUtil.decodeURL(spec.substring(0, index)));
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid url: " + spec
                    + " (" + e + ")", e);
        }
        if (inAsar.isEmpty()) inAsar = "/";
        return asarURLPart + '!' + UrlUtil.encodeURL(inAsar);
    }

    private String parseRelativeSpec(URL url, String spec) {
        assert url.getProtocol().equals("asar");

        String ctxFile = url.getFile();
        // if the spec begins with /, chop up the jar back !/
        if (spec.startsWith("/")) {
            int bangSlash = ctxFile.indexOf("!/");
            if (bangSlash == -1) {
                throw new IllegalArgumentException("malformed " +
                        "context url:" + url + ": no !/");
            }
            ctxFile = ctxFile.substring(0, bangSlash + 1);
        }
        // if asar:...../some + some2, will be asar:...../some2
        if (!ctxFile.endsWith("/") && !spec.startsWith("/")) {
            // chop up the last component
            int lastSlash = ctxFile.lastIndexOf('/');
            if (lastSlash == -1) {
                throw new IllegalArgumentException("malformed " +
                        "context url:" + url);
            }
            ctxFile = ctxFile.substring(0, lastSlash + 1);
        }
        return (ctxFile + spec);
    }
}
