package io.pacworx.atp.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class ClosedBeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    private String gmail;
    private String appleId;
    private String finding;
    private LocalDateTime registerDate;
    private LocalDateTime gmailSendDate;
    private LocalDateTime appleSendDate;

    public long getId() {
        return id;
    }

    public String getGmail() {
        return gmail;
    }

    public void setGmail(String gmail) {
        this.gmail = gmail;
    }

    public String getAppleId() {
        return appleId;
    }

    public void setAppleId(String appleId) {
        this.appleId = appleId;
    }

    public String getFinding() {
        return finding;
    }

    public void setFinding(String finding) {
        this.finding = finding;
    }

    public LocalDateTime getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(LocalDateTime registerDate) {
        this.registerDate = registerDate;
    }

    public LocalDateTime getGmailSendDate() {
        return gmailSendDate;
    }

    public void setGmailSendDate(LocalDateTime gmailSendDate) {
        this.gmailSendDate = gmailSendDate;
    }

    public LocalDateTime getAppleSendDate() {
        return appleSendDate;
    }

    public void setAppleSendDate(LocalDateTime appleSendDate) {
        this.appleSendDate = appleSendDate;
    }
}
