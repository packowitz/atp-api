package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.pacworx.atp.autotrade.domain.TradeStatus.DONE;

@Entity(name = "trade_path")
public class TradePath implements Serializable {
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
    private int maxSteps;
    private String startCurrency;
    private double startAmount;
    private String destCurrency;
    private Double destAmount;
    private boolean autoRestart;
    private ZonedDateTime startDate;
    private ZonedDateTime finishDate;
    @Transient
    private List<TradeStep> steps;

    public TradePath() {}

    public TradePath(TradePath origPath) {
        this.planId = origPath.planId;
        this.accountId = origPath.accountId;
        this.status = TradePlanStatus.ACTIVE;
        this.maxSteps = origPath.maxSteps;
        this.startCurrency = origPath.startCurrency;
        this.startAmount = origPath.startAmount;
        this.destCurrency = origPath.destCurrency;
        this.autoRestart = origPath.autoRestart;
        this.setStartDate(ZonedDateTime.now());
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

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getStepsCompleted() {
        if(steps != null) {
            return (int)steps.stream().filter(t -> t.getStatus() == DONE).count();
        }
        return 0;
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

    public String getDestCurrency() {
        return destCurrency;
    }

    public void setDestCurrency(String destCurrency) {
        this.destCurrency = destCurrency;
    }

    public Double getDestAmount() {
        return destAmount;
    }

    public void setDestAmount(Double destAmount) {
        this.destAmount = destAmount;
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

    @JsonIgnore
    public TradeStep getLatestStep() {
        if(this.steps == null || this.steps.isEmpty()) {
            return null;
        } else {
            return this.steps.get(0);
        }
    }

    @JsonIgnore
    public TradeStep getFirstStep() {
        if(this.steps != null) {
            for(TradeStep step: steps) {
                if(step.getStep() == 1) {
                    return step;
                }
            }
        }
        return null;
    }

    public List<TradeStep> getSteps() {
        return steps;
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
