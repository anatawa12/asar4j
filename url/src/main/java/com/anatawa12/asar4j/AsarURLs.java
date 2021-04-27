package com.anatawa12.asar4j;

import com.anatawa12.asar4j.internal.asar.Handler;
import com.anatawa12.asar4j.internal.asar.UrlUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

/**
 * static utility class for Asar URL Protocol.
 */
public class AsarURLs {
    private AsarURLs() {
    }

    /**
     * The URLStreamHandlerFactory only for asar.
     * If possible, you should use {@link #getProtocolHandlerPackage} or {@link #registerToSystemProp}
     */
    public static URLStreamHandlerFactory factory =
            (protocol) -> protocol.equalsIgnoreCase("asar") ? new Handler() : null;

    public static URL createUrl(URL asarFile, String path) throws MalformedURLException {
        return new URL("asar:" + UrlUtil.encodeURL(asarFile.toString()) + "!/" + path);
    }

    private static final String protocolHandlerPackage;

    // for shadowing
    static {
        String name = Handler.class.getName();
        protocolHandlerPackage = name.substring(0, name.length() - ".asar.Handler".length());
    }

    public static String getProtocolHandlerPackage() {
        return protocolHandlerPackage;
    }

    private static final String protocolPathProp = "java.protocol.handler.pkgs";

    public static void registerToSystemProp() {
        checkClassLoader();

        String value = System.getProperty(protocolPathProp);
        if (value == null || value.equals(""))
            System.setProperty(protocolPathProp, getProtocolHandlerPackage());
        else
            System.setProperty(protocolPathProp, value + "|" + getProtocolHandlerPackage());
    }

    private static void checkClassLoader() {
        if (Handler.class.getClassLoader() == null) return;
        try {
            if (ClassLoader.getSystemClassLoader().loadClass(Handler.class.getName()) == Handler.class) return;
        } catch (ClassNotFoundException ignored) {
        }
        throw new IllegalStateException("The handler will not be loaded with bootstrap or system class loader");
    }
}
