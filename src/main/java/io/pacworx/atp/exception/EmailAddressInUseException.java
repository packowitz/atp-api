package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class EmailAddressInUseException extends AtpException {

    public EmailAddressInUseException() {
        super(buildExceptionInfo());
    }

    public EmailAddressInUseException(String message) {
        super(message, buildExceptionInfo());
    }

    public EmailAddressInUseException(String message, Throwable cause) {
        super(message, cause, buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.BAD_REQUEST.value());
        info.enableShowRetryBtn();
        info.enableShowCloseBtn();
        info.setCustomTitle("Failed");
        info.setCustomMessage("The email address you named is already in use.");
        return info;
    }
}