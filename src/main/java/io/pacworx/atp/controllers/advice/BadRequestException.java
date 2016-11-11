package io.pacworx.atp.controllers.advice;

/**
 * Used to throw http bad request exceptions
 * Author: Max Tuzzolino
 */

public class BadRequestException extends RuntimeException {
    public BadRequestException() {}

    public BadRequestException(String message)
    {
        super(message);
    }
}
