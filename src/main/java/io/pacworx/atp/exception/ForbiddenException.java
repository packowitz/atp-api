package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AtpException {

    public ForbiddenException() {
        super(buildExceptionInfo());
    }

    public ForbiddenException(String message, String title) {
        super(message, buildExceptionInfo());
        setCustomMessage(message);
        setCustomTitle(title);
    }

    public ForbiddenException(String message) {
        super(message, buildExceptionInfo());
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause, buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.FORBIDDEN.value());
        info.enableShowResetAccountBtn();
        info.enableShowCloseBtn();
        return info;
    }
}