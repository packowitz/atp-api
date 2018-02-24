package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.pacworx.atp.autotrade.controller.BinanceController;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "trade_one_market")
public class TradeOneMarket implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @JsonIgnore
    @Column(name = "plan_id")
    private long planId;
    @JsonIgnore
    private long accountId;
    @Enumerated(EnumType.STRING)
    private TradePlanStatus status;
    private String symbol;
    private double minProfit;
    private String startCurrency;
    private double startAmount;
    private double balance = 0d;
    private boolean autoRestart;
    private ZonedDateTime startDate;
    private ZonedDateTime finishDate;
    @Transient
    private List<TradeStep> steps;

    public TradeOneMarket() {}

    public TradeOneMarket(TradePlan plan, BinanceController.CreateOneMarketRequest request) {
        planId = plan.getId();
        accountId = plan.getAccountId();
        status = TradePlanStatus.ACTIVE;
        symbol = request.symbol;
        minProfit = request.minProfit;
        startCurrency = request.startCurrency;
        startAmount = request.startAmount;
        autoRestart = request.autoRestart;
        startDate = ZonedDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPlanId() {
        return planId;
    }

    public void setPlanId(long planId) {
        this.planId = planId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public TradePlanStatus getStatus() {
        return status;
    }

    public void setStatus(TradePlanStatus status) {
        this.status = status;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getMinProfit() {
        return minProfit;
    }

    public void setMinProfit(double minProfit) {
        this.minProfit = minProfit;
    }

    public String getStartCurrency() {
        return startCurrency;
    }

    public void setStartCurrency(String startCurrency) {
        this.startCurrency = startCurrency;
    }

    public double getStartAmount() {
        return startAmount;
    }

    public void setStartAmount(double startAmount) {
        this.startAmount = startAmount;
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

    public boolean isAutoRestart() {
        return autoRestart;
    }

    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }

    public ZonedDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(ZonedDateTime startDate) {
        this.startDate = startDate;
    }

    public ZonedDateTime getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(ZonedDateTime finishDate) {
        this.finishDate = finishDate;
    }

    public List<TradeStep> getSteps() {
        return steps;
    }

    @JsonIgnore
    public TradeStep getActiveFirstStep() {
        if(steps != null) {
            for(TradeStep step: steps) {
                if(step.getStatus() == TradeStatus.ACTIVE && step.getStep() == 1) {
                    return step;
                }
            }
        }
        return null;
    }

    @JsonIgnore
    public TradeStep getLatesFirstStep() {
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
    public TradeStep getActiveStepBack() {
        if(steps != null) {
            for(TradeStep step: steps) {
                if(step.getStatus() == TradeStatus.ACTIVE && step.getStep() == 2) {
                    return step;
                }
            }
        }
        return null;
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
