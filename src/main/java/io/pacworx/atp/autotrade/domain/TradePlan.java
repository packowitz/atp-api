package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.ZonedDateTime;

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
    private double balance = 0d;
    private double balancePerc = 0d;
    private ZonedDateTime startDate;
    private ZonedDateTime lastActionDate;
    private ZonedDateTime finishDate;
    private int runsDone = 0;
    @Transient
    private TradePlanConfig config;

    public TradePlan() {}

    public TradePlan(TradeAccount account, TradePlanType type) {
        this.userId = account.getUserId();
        this.accountId = account.getId();
        this.type = type;
        this.status = TradePlanStatus.ACTIVE;
        this.startDate = ZonedDateTime.now();
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getBalancePerc() {
        return balancePerc;
    }

    public void setBalancePerc(double balancePerc) {
        this.balancePerc = balancePerc;
    }

    public void addBalancePerc(double balancePerc) {
        this.balancePerc += balancePerc;
    }

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(ZonedDateTime startedDate) {
        this.startDate = startedDate;
    }

    public ZonedDateTime getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(ZonedDateTime lastActionDate) {
        this.lastActionDate = lastActionDate;
    }

    public ZonedDateTime getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(ZonedDateTime finishDate) {
        this.finishDate = finishDate;
    }

    public int getRunsDone() {
        return runsDone;
    }

    public void setRunsDone(int runsDone) {
        this.runsDone = runsDone;
    }

    public void incRunsDone() {
        this.runsDone ++;
    }

    public TradePlanConfig getConfig() {
        return config;
    }

    public void setConfig(TradePlanConfig config) {
        this.config = config;
    }
}
