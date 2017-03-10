package io.pacworx.atp.exception;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LogManager.getLogger();

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = Exception.class)
    @ResponseBody ExceptionInfo
    handleBaseException(Exception e) {
        log.error(e.getMessage(), e);
        ExceptionInfo info = new ExceptionInfo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        info.enableShowCloseBtn();
        info.enableShowRetryBtn();
        return info;
    }

    /**
     * Generic ATP exception
     */
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = AtpException.class)
    @ResponseBody ExceptionInfo
    handleAtpException(AtpException e) {
        log.error(e.getMessage());
        return e.getExceptionInfo();
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = BadRequestException.class)
    @ResponseBody ExceptionInfo
    handleBadRequestException(BadRequestException e) {
        log.error(e.getMessage());
        return e.getExceptionInfo();
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = NotFoundException.class)
    @ResponseBody ExceptionInfo
    handleNotFoundException(NotFoundException e) {
        log.error(e.getMessage());
        return e.getExceptionInfo();
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(value = ForbiddenException.class)
    @ResponseBody ExceptionInfo
    handleForbiddenException(ForbiddenException e) {
        log.error(e.getMessage());
        return e.getExceptionInfo();
    }
}
