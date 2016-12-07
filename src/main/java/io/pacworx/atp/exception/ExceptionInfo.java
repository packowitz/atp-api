package io.pacworx.atp.exception;

import com.google.common.collect.ImmutableMap;
import org.springframework.http.HttpStatus;

public class ExceptionInfo {
    private ImmutableMap<Integer, String> TITLES = ImmutableMap.<Integer, String>builder()
            .put(HttpStatus.BAD_REQUEST.value(), "Bad Request")
            .put(HttpStatus.FORBIDDEN.value(), "Not Authorized")
            .put(HttpStatus.NOT_FOUND.value(), "Not Found")
            .put(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error")
            .build();
    private ImmutableMap<Integer, String> MESSAGES = ImmutableMap.<Integer, String>builder()
            .put(HttpStatus.BAD_REQUEST.value(), "We expected something else. Please make sure you have the latest version of ATP.")
            .put(HttpStatus.FORBIDDEN.value(), "You don't have the required permission to do this.")
            .put(HttpStatus.NOT_FOUND.value(), "The information you requested does not exist. Please make sure you have the latest version of ATP.")
            .put(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Congratulations! You found a way to break our application. We are working on that.")
            .build();
    private final int code;


    private String customTitle;
    private String customMessage;
    private boolean showRetryBtn;
    private boolean showResetAccountBtn;
    private boolean showCloseBtn;
    private boolean showHomeBtn;

    public ExceptionInfo(int code) {
        this.code = code;
    }

    public String getTitle() {
        return customTitle != null ? customTitle : TITLES.get(code);
    }

    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }

    public String getMessage() {
        return customMessage != null ? customMessage : MESSAGES.get(code);
    }

    public void setCustomMessage(String customMessage) {
        this.customMessage = customMessage;
    }

    public boolean getShowRetryBtn() {
        return showRetryBtn;
    }

    public void enableShowRetryBtn() {
        this.showRetryBtn = true;
    }

    public boolean getShowResetAccountBtn() {
        return showResetAccountBtn;
    }

    public void enableShowResetAccountBtn() {
        this.showResetAccountBtn = true;
    }

    public boolean getShowCloseBtn() {
        return showCloseBtn;
    }

    public void enableShowCloseBtn() {
        this.showCloseBtn = true;
    }

    public boolean getShowHomeBtn() {
        return showHomeBtn;
    }

    public void enableShowHomeBtn() {
        this.showHomeBtn = true;
    }
}