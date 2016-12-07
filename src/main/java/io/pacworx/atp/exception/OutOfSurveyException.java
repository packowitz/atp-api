package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class OutOfSurveyException extends AtpException {

    public OutOfSurveyException() {
        super(buildExceptionInfo());
    }

    public OutOfSurveyException(String message) {
        super(message, buildExceptionInfo());
    }

    public OutOfSurveyException(String message, Throwable cause) {
        super(message, cause, buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.NOT_FOUND.value());
        info.setCustomTitle("Congratulations");
        info.setCustomMessage("We are out of questions. Please come back tomorrow and thank you for answering so many ATPs.");
        info.enableShowHomeBtn();
        return info;
    }
}
