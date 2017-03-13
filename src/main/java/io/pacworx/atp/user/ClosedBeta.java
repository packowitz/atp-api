package io.pacworx.atp.user;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity
public class ClosedBeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String gmail;
    private String appleId;
    private String finding;
    private ZonedDateTime registerDate;
    private ZonedDateTime gmailSendDate;
    private ZonedDateTime appleSendDate;

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

    public ZonedDateTime getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(ZonedDateTime registerDate) {
        this.registerDate = registerDate;
    }

    public ZonedDateTime getGmailSendDate() {
        return gmailSendDate;
    }

    public void setGmailSendDate(ZonedDateTime gmailSendDate) {
        this.gmailSendDate = gmailSendDate;
    }

    public ZonedDateTime getAppleSendDate() {
        return appleSendDate;
    }

    public void setAppleSendDate(ZonedDateTime appleSendDate) {
        this.appleSendDate = appleSendDate;
    }
}
