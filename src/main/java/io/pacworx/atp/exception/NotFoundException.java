package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AtpException {

    public NotFoundException() {
        super(buildExceptionInfo());
    }

    public NotFoundException(String message, String title) {
        super(message, buildExceptionInfo());
        setCustomMessage(message);
        setCustomTitle(title);
    }

    public NotFoundException(String message) {
        super(message, buildExceptionInfo());
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause, buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.NOT_FOUND.value());
        info.enableShowRetryBtn();
        info.enableShowCloseBtn();
        return info;
    }
}