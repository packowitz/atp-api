package io.pacworx.atp.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.sql.Date;
import java.time.ZonedDateTime;

@Entity
public class Notification {
    @EmbeddedId
    @JsonIgnore
    private NotificationId id;
    @JsonIgnore
    private String os;
    @JsonView(Views.WebView.class)
    private String token;
    private boolean atpAnswerableEnabled = true;
    private ZonedDateTime atpAnswerableSendDate;
    private Date atpAnswerableBetweenTime;
    private boolean atpFinishedEnabled = true;
    private boolean announcementEnabled = true;
    private boolean feedbackEnabled = true;

    public Notification() {
    }

    public Notification(long userId, String deviceId) {
        this.id = new NotificationId(userId, deviceId);
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isAtpAnswerableEnabled() {
        return atpAnswerableEnabled;
    }

    public void setAtpAnswerableEnabled(boolean atpAnswerableEnabled) {
        this.atpAnswerableEnabled = atpAnswerableEnabled;
    }

    public ZonedDateTime getAtpAnswerableSendDate() {
        return atpAnswerableSendDate;
    }

    public void setAtpAnswerableSendDate(ZonedDateTime atpAnswerableSendDate) {
        this.atpAnswerableSendDate = atpAnswerableSendDate;
    }

    public Date getAtpAnswerableBetweenTime() {
        return atpAnswerableBetweenTime;
    }

    public void setAtpAnswerableBetweenTime(Date atpAnswerableBetweenTime) {
        this.atpAnswerableBetweenTime = atpAnswerableBetweenTime;
    }

    public boolean isAtpFinishedEnabled() {
        return atpFinishedEnabled;
    }

    public void setAtpFinishedEnabled(boolean atpFinishedEnabled) {
        this.atpFinishedEnabled = atpFinishedEnabled;
    }

    public boolean isAnnouncementEnabled() {
        return announcementEnabled;
    }

    public void setAnnouncementEnabled(boolean announcementEnabled) {
        this.announcementEnabled = announcementEnabled;
    }

    public boolean isFeedbackEnabled() {
        return feedbackEnabled;
    }

    public void setFeedbackEnabled(boolean feedbackEnabled) {
        this.feedbackEnabled = feedbackEnabled;
    }
}
