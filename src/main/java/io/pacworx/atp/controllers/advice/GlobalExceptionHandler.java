package io.pacworx.atp.controllers.advice;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global exception handler
 * Throwing these errors throughout the API will cause these to be executed
 * Author: Max Tuzzolino
 * Author: Philipp Schumacher
 */

@RestController
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LogManager.getLogger(GlobalExceptionHandler.class);

    private final static String GENERIC_ERROR = "Looks like you broke something. Good work.";
    private final static String NOT_FOUND_REASON = "The resource you requested cannot be found";
    private final static String FORBIDDEN_REASON = "You don't have the required permission to do this action.";
    private final static String BAD_REQUEST_REASON = "Your request didn't have a very good format.";

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = GENERIC_ERROR)
    @ExceptionHandler(value = Exception.class)
    public void handleBaseException(Exception e){
        LOGGER.error(e != null ? e.getMessage() : GENERIC_ERROR);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = NOT_FOUND_REASON)
    @ExceptionHandler(value = NotFoundException.class)
    public void handleNotFoundException(NotFoundException e){
        LOGGER.info(e != null ? e.getMessage() : NOT_FOUND_REASON);
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = FORBIDDEN_REASON)
    @ExceptionHandler(value = ForbiddenException.class)
    public void handleForbiddenException(ForbiddenException e){
        LOGGER.warn(e != null ? e.getMessage() : FORBIDDEN_REASON);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = BAD_REQUEST_REASON)
    @ExceptionHandler(value = BadRequestException.class)
    public void handleBadRequestException(BadRequestException e){
        LOGGER.info(e != null ? e.getMessage() : BAD_REQUEST_REASON);
    }
}
