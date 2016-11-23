package io.pacworx.atp.user;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ResponseWithUser<T> {
    private User user;
    private long timestamp;
    private T data;

    public ResponseWithUser(User user, T data) {
        this.user = user;
        this.timestamp = ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
        this.data = data;
    }

    public User getUser() {
        return user;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public T getData() {
        return data;
    }
}
