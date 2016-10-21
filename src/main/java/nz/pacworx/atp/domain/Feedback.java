package nz.pacworx.atp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @JsonIgnore
    private long userId;
    @Enumerated(EnumType.STRING)
    private FeedbackType type;
    @Enumerated(EnumType.STRING)
    private FeedbackStatus status;
    private ZonedDateTime sendDate;
    private ZonedDateTime lastActionDate;
    private String title;
    private String message;
    private int answers = 0;
    @JsonView(Views.AppView.class)
    private int unreadAnswers = 0;

    public void incUnreadAnswers() {
        this.unreadAnswers++;
    }

    public void incAnswers() {
        this.answers++;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public FeedbackType getType() {
        return type;
    }

    public void setType(FeedbackType type) {
        this.type = type;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public void setStatus(FeedbackStatus status) {
        this.status = status;
    }

    public ZonedDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(ZonedDateTime sendDate) {
        this.sendDate = sendDate;
        this.setLastActionDate(sendDate);
    }

    public ZonedDateTime getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(ZonedDateTime lastActionDate) {
        this.lastActionDate = lastActionDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getAnswers() {
        return answers;
    }

    public int getUnreadAnswers() {
        return unreadAnswers;
    }
}
