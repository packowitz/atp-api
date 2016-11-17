package io.pacworx.atp.controllers.advice;

import io.pacworx.atp.domain.ExceptionInfo;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    ExceptionInfo
    handleBaseException(HttpServletRequest req, Exception e){
        String message = e != null ? e.getMessage() : GENERIC_ERROR;
        LOGGER.error(message);

        return new ExceptionInfo(req.getRequestURL().toString(), message);
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = NotFoundException.class)
    @ResponseBody ExceptionInfo
    handleNotFoundException(HttpServletRequest req, NotFoundException e){
        String message = e != null ? e.getMessage() : NOT_FOUND_REASON;

        LOGGER.info(message);

        return new ExceptionInfo(req.getRequestURL().toString(), message);
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(value = ForbiddenException.class)
    @ResponseBody ExceptionInfo
    handleForbiddenException(HttpServletRequest req, ForbiddenException e){
        String message = e != null ? e.getMessage() : FORBIDDEN_REASON;

        LOGGER.info(message);

        return new ExceptionInfo(req.getRequestURL().toString(), message);
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = BadRequestException.class)
    @ResponseBody ExceptionInfo
    handleBadRequestException(HttpServletRequest req, BadRequestException e){
        String message = e != null ? e.getMessage() : BAD_REQUEST_REASON;

        LOGGER.info(message);

        return new ExceptionInfo(req.getRequestURL().toString(), message);
    }
}
