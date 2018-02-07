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
    TradePlanType planType;
    @Enumerated(EnumType.STRING)
    TradePlanStatus status;

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

    public TradePlanType getPlanType() {
        return planType;
    }

    public void setPlanType(TradePlanType type) {
        this.planType = type;
    }

    public TradePlanStatus getStatus() {
        return status;
    }

    public void setStatus(TradePlanStatus status) {
        this.status = status;
    }
}
