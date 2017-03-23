package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class LoginFailedException extends AtpException {

    public LoginFailedException() {
        super(buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.UNAUTHORIZED.value());
        info.setCustomTitle("Login failed");
        info.setCustomMessage("Either email/username or password is wrong");
        info.enableShowCloseBtn();
        return info;
    }
}
