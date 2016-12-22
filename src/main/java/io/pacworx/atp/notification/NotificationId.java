package io.pacworx.atp.notification;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class NotificationId implements Serializable {
    private long userId;
    private String deviceId;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
