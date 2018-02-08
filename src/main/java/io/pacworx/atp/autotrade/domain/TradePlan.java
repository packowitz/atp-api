package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
public class TradePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @JsonIgnore
    private long userId;
    @JsonIgnore
    private long accountId;
    @Enumerated(EnumType.STRING)
    TradePlanType type;
    @Enumerated(EnumType.STRING)
    TradePlanStatus status;
    private String description;

    public TradePlan() {}

    public TradePlan(TradeAccount account, TradePlanType type) {
        this.userId = account.getUserId();
        this.accountId = account.getId();
        this.type = type;
        this.status = TradePlanStatus.ACTIVE;
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

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public TradePlanType getType() {
        return type;
    }

    public void setType(TradePlanType type) {
        this.type = type;
    }

    public TradePlanStatus getStatus() {
        return status;
    }

    public void setStatus(TradePlanStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
