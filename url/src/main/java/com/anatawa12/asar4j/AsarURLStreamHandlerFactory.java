package com.anatawa12.asar4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class AsarURLStreamHandlerFactory implements URLStreamHandlerFactory {
    private AsarURLStreamHandlerFactory() {
    }

    public static AsarURLStreamHandlerFactory INSTANCE = new AsarURLStreamHandlerFactory();

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.equalsIgnoreCase("asar"))
            return new AsarURLHandler();
        return null;
    }

    public static URL createUrl(URL asarFile, String path) throws MalformedURLException {
        return new URL("asar:" + UrlUtil.encodeURL(asarFile.toString()) + "!/" + path);
    }
}
