package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class TradeCircle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @JsonIgnore
    private long planId;
    @Enumerated(EnumType.STRING)
    private TradePlanStatus status;
    private Integer activeStep;
    private Long activeOrderId;
    private String startCurrency;
    private double startAmount;
    private Double finishAmount;
    @Enumerated(EnumType.STRING)
    private TradeCircleRisk risk;
    private int treshold;
    private boolean cancelOnTreshold;
    private ZonedDateTime startDate;
    private ZonedDateTime finishDate;
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "circle_id")
    @OrderBy("step asc")
    private List<TradeCircleStep> steps;

    public long getId() {
        return id;
    }

    public long getPlanId() {
        return planId;
    }

    public void setPlanId(long planId) {
        this.planId = planId;
    }

    public TradePlanStatus getStatus() {
        return status;
    }

    public void setStatus(TradePlanStatus status) {
        this.status = status;
    }

    public Integer getActiveStep() {
        return activeStep;
    }

    public void setActiveStep(Integer activeStep) {
        this.activeStep = activeStep;
    }

    public Long getActiveOrderId() {
        return activeOrderId;
    }

    public void setActiveOrderId(Long activeOrderId) {
        this.activeOrderId = activeOrderId;
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

    public Double getFinishAmount() {
        return finishAmount;
    }

    public void setFinishAmount(Double finishAmount) {
        this.finishAmount = finishAmount;
    }

    public TradeCircleRisk getRisk() {
        return risk;
    }

    public void setRisk(TradeCircleRisk risk) {
        this.risk = risk;
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
    public TradeCircleStep getCurrentStep() {
        return this.steps.get(activeStep - 1);
    }

    @JsonIgnore
    public TradeCircleStep getNextStep() {
        if(this.steps.size() > activeStep) {
            return this.steps.get(activeStep);
        }
        return null;
    }

    public List<TradeCircleStep> getSteps() {
        return steps;
    }

    public void setSteps(List<TradeCircleStep> steps) {
        this.steps = steps;
    }

    public void addStep(TradeCircleStep step) {
        if(this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(step);
    }
}
