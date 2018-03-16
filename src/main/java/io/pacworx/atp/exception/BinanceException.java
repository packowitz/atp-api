package io.pacworx.atp.exception;

public class BinanceException extends RuntimeException {
    private int code;
    private String msg;

    public BinanceException() {
        super();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
