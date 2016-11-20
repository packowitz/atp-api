package io.pacworx.atp.exception;

/**
 * Customized error messaging class
 * Author: Max Tuzzolino
 */

public class ExceptionInfo {
    public final String url;
    public final String message;

    public ExceptionInfo(String url, String message) {
        this.url = url;
        this.message = message;
    }
}