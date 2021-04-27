package com.anatawa12.asar4j;

public class FactoryRegistrar {
    static {
        AsarURLs.registerToSystemProp();
    }

    // run in static initializer
    public static void register() {

    }
}
