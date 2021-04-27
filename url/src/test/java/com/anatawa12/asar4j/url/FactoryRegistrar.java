package com.anatawa12.asar4j.url;

import java.net.URL;

public class FactoryRegistrar {
    static {
        URL.setURLStreamHandlerFactory(AsarURLStreamHandlerFactory.INSTANCE);
    }

    // run in static initializer
    public static void register() {

    }
}
