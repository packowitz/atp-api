package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends AtpException {

    public InternalServerException() {
        super(buildExceptionInfo());
    }

    public InternalServerException(String message) {
        super(message, buildExceptionInfo());
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause, buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        info.enableShowHomeBtn();
        info.enableShowRetryBtn();
        return info;
    }
}
