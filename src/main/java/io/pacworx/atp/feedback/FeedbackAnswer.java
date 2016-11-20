package io.pacworx.atp.feedback;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity
public class FeedbackAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @JsonIgnore
    private long feedbackId;
    @JsonIgnore
    private long userId;
    @JsonView(Views.WebView.class)
    private Long adminId;
    private boolean readAnswer;
    private ZonedDateTime sendDate;
    private String message;

    public boolean isAdminAnswer() {
        return adminId != null;
    }

    public long getId() {
        return id;
    }

    public long getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public boolean isReadAnswer() {
        return readAnswer;
    }

    public void setReadAnswer(boolean readAnswer) {
        this.readAnswer = readAnswer;
    }

    public ZonedDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(ZonedDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
