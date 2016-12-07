package io.pacworx.atp.exception;

public class AtpException extends RuntimeException {
    private final ExceptionInfo exceptionInfo;

    public AtpException(ExceptionInfo exceptionInfo) {
        super();
        this.exceptionInfo = exceptionInfo;
    }

    public AtpException(String message, ExceptionInfo exceptionInfo) {
        super(message);
        this.exceptionInfo = exceptionInfo;
    }

    public AtpException(String message, Throwable cause, ExceptionInfo exceptionInfo) {
        super(message, cause);
        this.exceptionInfo = exceptionInfo;
    }

    public ExceptionInfo getExceptionInfo() {
        return exceptionInfo;
    }

    public void setCustomTitle(String customTitle) {
        this.exceptionInfo.setCustomTitle(customTitle);
    }

    public void setCustomMessage(String customMessage) {
        this.exceptionInfo.setCustomMessage(customMessage);
    }
}
