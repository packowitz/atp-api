package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "trade_plan")
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
    @Transient
    private List<TradeStep> steps;

    public TradePlan() {}

    public TradePlan(TradeAccount account, TradePlanType type) {
        this.userId = account.getUserId();
        this.accountId = account.getId();
        this.type = type;
        this.status = TradePlanStatus.ACTIVE;
        this.startDate = ZonedDateTime.now();
    }

    public void cancel() {
        this.status = TradePlanStatus.CANCELLED;
        this.finishDate = ZonedDateTime.now();
    }

    public void finish() {
        this.status = TradePlanStatus.FINISHED;
        this.finishDate = ZonedDateTime.now();
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

    public void addBalance(double balance) {
        this.balance += balance;
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

    public List<TradeStep> getSteps() {
        return steps;
    }

    @JsonIgnore
    public TradeStep getActiveFirstStep() {
        if(steps != null) {
            for(TradeStep step: steps) {
                if((step.getStatus() == TradeStatus.ACTIVE || step.isNeedRestart()) && step.getStep() == 1) {
                    return step;
                }
            }
        }
        return null;
    }

    @JsonIgnore
    public TradeStep getLatestFirstStep() {
        if(steps != null) {
            for(TradeStep step: steps) {
                if(step.getStep() == 1) {
                    return step;
                }
            }
        }
        return null;
    }

    @JsonIgnore
    public TradeStep getLatestPausedStep() {
        if(steps != null) {
            for(TradeStep step: steps) {
                if(step.getStatus() == TradeStatus.PAUSED) {
                    return step;
                }
            }
        }
        return null;
    }

    @JsonIgnore
    public TradeStep getActiveStep(int stepNumber) {
        if(steps != null) {
            for(TradeStep step: steps) {
                if((step.getStatus() == TradeStatus.ACTIVE || step.isNeedRestart()) && step.getStep() == stepNumber) {
                    return step;
                }
            }
        }
        return null;
    }

    @JsonIgnore
    public List<TradeStep> getActiveSteps() {
        if(steps != null) {
            return steps.stream().filter(s -> s.getStatus() == TradeStatus.ACTIVE || s.isNeedRestart()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public void setSteps(List<TradeStep> steps) {
        this.steps = steps;
    }

    public void addStep(TradeStep step) {
        if(this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(0, step);
    }
}
