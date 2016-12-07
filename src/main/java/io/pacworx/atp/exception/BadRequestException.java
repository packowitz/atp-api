package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AtpException {

    public BadRequestException() {
        super(buildExceptionInfo());
    }

    public BadRequestException(String message) {
        super(message, buildExceptionInfo());
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause, buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.BAD_REQUEST.value());
        info.enableShowRetryBtn();
        info.enableShowCloseBtn();
        return info;
    }
}
