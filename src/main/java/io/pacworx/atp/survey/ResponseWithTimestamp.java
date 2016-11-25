package io.pacworx.atp.survey;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ResponseWithTimestamp<T> {
    private T data;
    private long timestamp;

    public ResponseWithTimestamp(T data) {
        this.data = data;
        this.timestamp = ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
