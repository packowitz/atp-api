package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.pacworx.atp.exception.BinanceException;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.ZonedDateTime;

@Entity
public class TradeAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @JsonIgnore
    private long planId;
    @JsonIgnore
    private long subplanId;
    @JsonIgnore
    private long stepId;
    private ZonedDateTime timestamp;
    private String level;
    private String title;
    private String message;

    public TradeAuditLog() {}

    public TradeAuditLog(TradeStep step, BinanceException e) {
        stepId = step.getId();
        subplanId = step.getSubplanId();
        planId = step.getPlanId();
        level = "ERROR";
        timestamp = ZonedDateTime.now();
        title = "Binance Error for " + step.getOrderId();
        message = e.getCode() + " - " + e.getMsg();
    }

    public long getId() {
        return id;
    }

    public long getPlanId() {
        return planId;
    }

    public void setPlanId(long planId) {
        this.planId = planId;
    }

    public long getSubplanId() {
        return subplanId;
    }

    public void setSubplanId(long subplanId) {
        this.subplanId = subplanId;
    }

    public long getStepId() {
        return stepId;
    }

    public void setStepId(long stepId) {
        this.stepId = stepId;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
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
