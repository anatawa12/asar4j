package com.anatawa12.asar4j;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public abstract class AsarURLConnection extends URLConnection {
    protected URL asarFileURL;
    protected String entryName;

    @SuppressWarnings("RedundantThrows")
    public AsarURLConnection(URL url) throws IOException {
        super(url);
    }

    public final URL getAsarFileURL() {
        return asarFileURL;
    }

    public final String getEntryName() {
        return entryName;
    }

    public abstract AsarFile getAsarFile() throws IOException;

    public abstract AsarEntry getAsarEntry() throws IOException;
}
