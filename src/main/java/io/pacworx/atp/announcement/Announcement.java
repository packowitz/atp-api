package io.pacworx.atp.announcement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import io.pacworx.atp.config.Views;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @JsonView(Views.WebView.class)
    private long adminId;
    private ZonedDateTime sendDate;
    @JsonView(Views.WebView.class)
    private String countries;
    @JsonIgnore
    private String title;
    private String message;

    public long getId() {
        return id;
    }

    public long getAdminId() {
        return adminId;
    }

    public void setAdminId(long adminId) {
        this.adminId = adminId;
    }

    public ZonedDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(ZonedDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public String getCountries() {
        return countries;
    }

    public void setCountries(String countries) {
        this.countries = countries;
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
}
