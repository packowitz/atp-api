package io.pacworx.atp.notification;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.sql.Time;
import java.time.ZonedDateTime;
import java.util.Calendar;

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
    @JsonIgnore
    private Time atpAnswerableBetweenTime;
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

    public int getHoursBetweenAnswerable() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.atpAnswerableBetweenTime);
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        if(hours == 23 && cal.get(Calendar.MINUTE) > 0) {
            return 24;
        }
        return hours;
    }

    public Time getAtpAnswerableBetweenTime() {
        return atpAnswerableBetweenTime;
    }

    public void setAtpAnswerableBetweenTime(Time atpAnswerableBetweenTime) {
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
