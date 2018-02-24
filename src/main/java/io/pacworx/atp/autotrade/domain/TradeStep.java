package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity(name = "trade_step")
public class TradeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;
    @Column(name = "plan_id")
    @JsonIgnore
    private long planId;
    @Column(name = "subplan_id")
    @JsonIgnore
    private long subplanId;
    private int step;
    private Long orderId;
    /** Indicates how much was traded in the current order. Set to 0 for every new order */
    private double orderFilled = 0d;
    private Double orderAltcoinQty;
    private Double orderBasecoinQty;
    @Enumerated(EnumType.STRING)
    private TradeStatus status;
    private String symbol;
    private String side;
    private double price;
    private Double priceThreshold;
    private String inCurrency;
    /** Possible amount to trade with. */
    private Double inAmount;
    /** Indicates how much was traded in the current step. Set to 0 every time a new step is created */
    private double inFilled = 0d;
    private String outCurrency;
    /** Amount of traded outCurrency. Usually the inAmount for the next step. */
    private double outAmount = 0d;
    private ZonedDateTime startDate;
    private ZonedDateTime finishDate;
    @Transient
    @JsonIgnore
    private boolean dirty = false;

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

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public double getOrderFilled() {
        return orderFilled;
    }

    public void setOrderFilled(double orderFilled) {
        this.orderFilled = orderFilled;
    }

    public void addOrderFilled(double orderFilled) {
        this.orderFilled += orderFilled;
    }

    public Double getOrderAltcoinQty() {
        return orderAltcoinQty;
    }

    public void setOrderAltcoinQty(Double orderAltcoinQty) {
        this.orderAltcoinQty = orderAltcoinQty;
    }

    public Double getOrderBasecoinQty() {
        return orderBasecoinQty;
    }

    public void setOrderBasecoinQty(Double orderBasecoinQty) {
        this.orderBasecoinQty = orderBasecoinQty;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Double getPriceThreshold() {
        return priceThreshold;
    }

    public void setPriceThreshold(Double priceThreshold) {
        this.priceThreshold = priceThreshold;
    }

    public String getInCurrency() {
        return inCurrency;
    }

    public void setInCurrency(String inCurrency) {
        this.inCurrency = inCurrency;
    }

    public Double getInAmount() {
        return inAmount;
    }

    public void setInAmount(Double inAmount) {
        this.inAmount = inAmount;
    }

    public double getInFilled() {
        return inFilled;
    }

    public void setInFilled(double inFilled) {
        this.inFilled = inFilled;
    }

    public void addInFilled(double inFilled) {
        this.inFilled += inFilled;
    }

    public String getOutCurrency() {
        return outCurrency;
    }

    public void setOutCurrency(String outCurrency) {
        this.outCurrency = outCurrency;
    }

    public double getOutAmount() {
        return outAmount;
    }

    public void setOutAmount(double outAmount) {
        this.outAmount = outAmount;
    }

    public void addOutAmount(double outFilled) {
        this.outAmount += outFilled;
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

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        this.dirty = true;
    }
}
