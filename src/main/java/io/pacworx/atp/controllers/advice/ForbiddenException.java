package io.pacworx.atp.controllers.advice;

/**
 * Used to throw http forbidden exceptions
 * Author: Max Tuzzolino
 */

public class ForbiddenException extends RuntimeException {
    public ForbiddenException() {}

    public ForbiddenException(String message)
    {
        super(message);
    }
}
