package io.pacworx.atp.exception;

/**
 * Used to throw http not found exceptions
 * Author: Max Tuzzolino
 */

public class NotFoundException extends RuntimeException {
    public NotFoundException() {}

    public NotFoundException(String message)
    {
        super(message);
    }
}
