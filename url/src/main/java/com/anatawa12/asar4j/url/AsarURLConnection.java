package com.anatawa12.asar4j.url;

import com.anatawa12.asar4j.AsarEntry;
import com.anatawa12.asar4j.AsarFile;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;

public class AsarURLConnection extends URLConnection {
    private URL asarFileURL;
    private String entryName;
    private URLConnection asarFileConnection;

    private AsarFile asarFile;
    private AsarEntry asarEntry;

    private boolean useCaches;

    public AsarURLConnection(URL url) throws IOException {
        super(url);
        parseSpec(url.getFile());
        asarFileConnection = asarFileURL.openConnection();
    }

    private void parseSpec(String spec) throws MalformedURLException {
        int sep = spec.indexOf("!/");
        if (sep < 0)
            throw new MalformedURLException("no !/ found in url spec: " + spec);

        asarFileURL = new URL(UrlUtil.decodeURL(spec.substring(0, sep)));
        sep++;
        entryName = sep + 1 != spec.length() ? UrlUtil.decodeURL(spec.substring(sep + 1)) : null;
    }

    public final URL getAsarFileURL() {
        return asarFileURL;
    }

    public final String getEntryName() {
        return entryName;
    }

    public AsarFile getAsarFile() throws IOException {
        connect();
        return asarFile;
    }

    public AsarEntry getAsarEntry() throws IOException {
        connect();
        return asarEntry;
    }


    @Override
    public Permission getPermission() throws IOException {
        return asarFileConnection.getPermission();
    }

    @Override
    public void connect() throws IOException {
        if (connected) return;

        useCaches = getUseCaches();
        asarFile = AsarFileFactory.INSTANCE.getAsarFile(getAsarFileURL(), useCaches);

        if (entryName != null) {
            asarEntry = asarFile.getEntry(entryName);
            if (asarEntry == null) {
                if (!useCaches) asarFile.close();
                throw new FileNotFoundException("asar entry " + entryName +
                        " not found in " + asarFileURL);
            }
        }
        //noinspection ConstantConditions
        assert entryName == null || asarEntry != null;
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        if (entryName == null)
            throw new IOException("no entry name specified");

        return new AsarURLInputStream(asarFile.getInputStream(asarEntry));
    }

    private class AsarURLInputStream extends FilterInputStream {
        protected AsarURLInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (!useCaches) asarFile.close();
            }
        }
    }

    @Override
    public long getContentLengthLong() {
        try {
            connect();
            if (asarEntry == null)
                return asarFileConnection.getContentLengthLong();
            else
                return asarEntry.getSize();
        } catch (IOException ignored) {
        }
        return -1;
    }

    @Override
    public Object getContent() throws IOException {
        connect();
        return entryName == null ? asarFile : super.getContent();
    }

    private String contentType;

    @Override
    public String getContentType() {
        if (contentType != null) return contentType;
        // see github.com/sindresorhus/file-type/pull/378. it uses application/x-asar
        if (entryName == null) return contentType = "application/x-asar";
        try {
            connect();
            InputStream stream = asarFile.getInputStream(asarEntry);
            contentType = guessContentTypeFromStream(new BufferedInputStream(stream));
            stream.close();
        } catch (IOException ignored) {
        }
        if (contentType == null)
            contentType = guessContentTypeFromName(entryName);
        if (contentType == null)
            contentType = "content/unknown";
        return contentType;
    }
}
