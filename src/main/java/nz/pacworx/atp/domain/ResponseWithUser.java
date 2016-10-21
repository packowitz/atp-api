package nz.pacworx.atp.domain;

public class ResponseWithUser<T> {
    private User user;
    private T data;

    public ResponseWithUser() {}

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
