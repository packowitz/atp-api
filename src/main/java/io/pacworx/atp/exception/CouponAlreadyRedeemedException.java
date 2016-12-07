package io.pacworx.atp.exception;

import org.springframework.http.HttpStatus;

public class CouponAlreadyRedeemedException extends AtpException {

    public CouponAlreadyRedeemedException() {
        super(buildExceptionInfo());
    }

    public CouponAlreadyRedeemedException(String message) {
        super(message, buildExceptionInfo());
    }

    public CouponAlreadyRedeemedException(String message, Throwable cause) {
        super(message, cause, buildExceptionInfo());
    }

    private static ExceptionInfo buildExceptionInfo() {
        ExceptionInfo info = new ExceptionInfo(HttpStatus.FORBIDDEN.value());
        info.setCustomTitle("Already redeemed");
        info.setCustomMessage("You can redeem each coupon only once.");
        info.enableShowCloseBtn();
        return info;
    }
}