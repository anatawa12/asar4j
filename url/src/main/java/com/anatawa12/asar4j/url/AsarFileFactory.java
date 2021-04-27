package com.anatawa12.asar4j.url;

import com.anatawa12.asar4j.AsarFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

class AsarFileFactory {
    static final AsarFileFactory INSTANCE = new AsarFileFactory();

    private static final HashMap<URL, AsarFile> fileCache = new HashMap<>();

    AsarFile getAsarFile(URL url, boolean useCache) throws IOException {
        AsarFile result;
        if (useCache) {
            synchronized (INSTANCE) {
                result = fileCache.get(url);
            }
            if (result == null) {
                // create new for cache
                AsarFile created = createURLAsarFile(url);
                synchronized (INSTANCE) {
                    result = fileCache.get(url);
                    if (result == null) {
                        fileCache.put(url, created);
                        result = created;
                    } else {
                        created.close();
                    }
                }
            }
        } else {
            result = createURLAsarFile(url);
        }
        return result;
    }

    private AsarFile createURLAsarFile(URL url) throws IOException {
        if (isLocalFileUrl(url)) {
            try {
                return new AsarFile(Paths.get(new URI("file:" + url.getFile())));
            } catch (URISyntaxException e) {
                throw new AssertionError(e);
            }
        } else {
            return getAndCreate(url);
        }
    }

    private AsarFile getAndCreate(URL url) throws IOException {
        try (InputStream stream = url.openConnection().getInputStream()) {
            Path tmpFile = Files.createTempFile("asar-cache", null);
            try {
                Files.copy(stream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
                AsarFile URLAsarFile = new AsarFile(tmpFile);
                tmpFile.toFile().deleteOnExit();
                return URLAsarFile;
            } catch (Throwable t) {
                try {
                    Files.delete(tmpFile);
                } catch (Throwable deleting) {
                    t.addSuppressed(deleting);
                }
                throw t;
            }
        }
    }

    private boolean isLocalFileUrl(URL url) {
        if (url.getProtocol().equalsIgnoreCase("file")) {
            return url.getHost() == null
                    || url.getHost().equals("")
                    || url.getHost().equals("~")
                    || url.getHost().equalsIgnoreCase("localhost");
        }
        return false;
    }
}
