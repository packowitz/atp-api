package io.pacworx.atp.user;

public class ResponseWithUser<T> {
    private User user;
    private T data;

    public ResponseWithUser(User user, T data) {
        this.user = user;
        this.data = data;
    }

    public User getUser() {
        return user;
    }

    public T getData() {
        return data;
    }
}
