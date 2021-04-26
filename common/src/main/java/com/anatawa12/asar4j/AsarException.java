package com.anatawa12.asar4j;

import java.io.IOException;

public class AsarException extends IOException {
    private static final long serialVersionUID = 1;

    public AsarException() {
    }

    public AsarException(String message) {
        super(message);
    }

    public AsarException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsarException(Throwable cause) {
        super(cause);
    }
}
