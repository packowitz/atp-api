package io.pacworx.atp.autotrade.domain;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
public class TradeOrderObserver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long orderId;
    private String symbol;
    private String broker;
    private long userId;
    private long accountId;
    private long planId;
    @Enumerated(EnumType.STRING)
    private TradePlanType planType;
    private long subplanId;
    private int treshold;
    private boolean cancelOnTreshold;
    private ZonedDateTime checkDate;

    public long getId() {
        return id;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
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

    public long getPlanId() {
        return planId;
    }

    public void setPlanId(long planId) {
        this.planId = planId;
    }

    public TradePlanType getPlanType() {
        return planType;
    }

    public void setPlanType(TradePlanType planType) {
        this.planType = planType;
    }

    public long getSubplanId() {
        return subplanId;
    }

    public void setSubplanId(long subplanId) {
        this.subplanId = subplanId;
    }

    public int getTreshold() {
        return treshold;
    }

    public void setTreshold(int treshold) {
        this.treshold = treshold;
    }

    public boolean isCancelOnTreshold() {
        return cancelOnTreshold;
    }

    public void setCancelOnTreshold(boolean cancelOnTreshold) {
        this.cancelOnTreshold = cancelOnTreshold;
    }

    public ZonedDateTime getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(ZonedDateTime checkDate) {
        this.checkDate = checkDate;
    }
}
