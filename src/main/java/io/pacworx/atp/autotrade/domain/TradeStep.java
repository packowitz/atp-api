package io.pacworx.atp.autotrade.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.autotrade.service.TradeUtil;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "trade_step")
public class TradeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "plan_id")
    @JsonIgnore
    private long planId;
    /** Indicates where this step is in your algorithm **/
    private int step;
    /** last known orderId **/
    private Long orderId;
    /** Indicates how much was traded in the current order. Set to 0 for every new order */
    private double orderFilled = 0d;
    /** How many altcoins are in the current order **/
    private Double orderAltcoinQty;
    /** How many basecoins (BTC, ETH or BNB) are in the current order **/
    private Double orderBasecoinQty;
    @Enumerated(EnumType.STRING)
    private TradeStatus status;
    /** Market. e.g. LTCBTC **/
    private String symbol;
    /** BUY or SELL **/
    private String side;
    /** Price of last order **/
    private double price;
    /** Optional. Threshold used by price calculations **/
    private Double priceThreshold;
    /** Currency you want to give **/
    private String inCurrency;
    /** Possible amount to trade with. */
    private Double inAmount;
    /** Indicates how much was traded in the current step. Set to 0 every time a new step is created */
    private double inFilled = 0d;
    /** Currency you want to get **/
    private String outCurrency;
    /** Amount of traded outCurrency. Usually the inAmount for the next step. */
    private double outAmount = 0d;
    /** When was this step created **/
    private ZonedDateTime startDate;
    /** When was the last check of the market this step is trading in **/
    private ZonedDateTime checkedMarketDate;
    /** When was step finished. NULL as long as it is active. **/
    private ZonedDateTime finishDate;
    /** Indicates that something went wrong starting this step and it should try to restart again **/
    private boolean needRestart = false;
    /** temporary indicator if the step has changed and needs to be saved **/
    @Transient
    @JsonIgnore
    private boolean dirty = false;
    @Transient
    @JsonIgnore
    private List<TradeAuditLog> newAuditLogs;
    @Transient
    @JsonIgnore
    private double newFilling = 0d;

    public void cancel() {
        this.status = TradeStatus.CANCELLED;
        this.needRestart = false;
        this.finishDate = ZonedDateTime.now();
        this.dirty = true;
    }

    public void finish() {
        this.status = TradeStatus.DONE;
        this.needRestart = false;
        this.finishDate = ZonedDateTime.now();
        this.dirty = true;
    }

    public void pause() {
        this.status = TradeStatus.PAUSED;
        this.orderId = null;
        this.needRestart = false;
        this.dirty = true;
    }

    public void setTradingMarket(String symbol) {
        this.symbol = symbol;
        this.side = TradeUtil.getSideOfMarket(symbol, inCurrency);
        this.outCurrency = TradeUtil.otherCur(symbol, inCurrency);
    }

    public void calcFilling(BinanceOrderResult orderResult) {
        double executedAltCoin = Double.parseDouble(orderResult.getExecutedQty()) - orderFilled;
        double executedBaseCoin = executedAltCoin * Double.parseDouble(orderResult.getPrice());
        if(TradeUtil.isBuy(side)) {
            addInFilled(executedBaseCoin);
            addOutAmount(executedAltCoin);
        } else {
            addInFilled(executedAltCoin);
            addOutAmount(executedBaseCoin);
        }
        this.orderFilled += executedAltCoin;
        this.newFilling = executedAltCoin;
        this.dirty = true;
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

    public ZonedDateTime getCheckedMarketDate() {
        return checkedMarketDate;
    }

    public void setCheckedMarketDate(ZonedDateTime checkedMarketDate) {
        this.checkedMarketDate = checkedMarketDate;
    }

    public ZonedDateTime getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(ZonedDateTime finishDate) {
        this.finishDate = finishDate;
    }

    public boolean isNeedRestart() {
        return needRestart;
    }

    public void setNeedRestart(boolean needRestart) {
        this.needRestart = needRestart;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty() {
        this.dirty = true;
    }

    public List<TradeAuditLog> getNewAuditLogs() {
        return newAuditLogs;
    }

    public void addAuditLog(TradeAuditLog log) {
        if(this.newAuditLogs == null) {
            this.newAuditLogs = new ArrayList<>();
        }
        this.newAuditLogs.add(log);
        this.dirty = true;
    }

    public void addInfoAuditLog(String title) {
        this.addInfoAuditLog(title, null);
    }

    public void addInfoAuditLog(String title, String message) {
        this.addAuditLog("INFO", title, message);
    }

    public void addErrorAuditLog(String title, String message) {
        this.addAuditLog("ERROR", title, message);
    }

    private void addAuditLog(String level, String title, String message) {
        TradeAuditLog newAuditLog = new TradeAuditLog();
        //ids must be set before saving
        newAuditLog.setTimestamp(ZonedDateTime.now());
        newAuditLog.setLevel(level);
        newAuditLog.setTitle(title);
        newAuditLog.setMessage(message);

        addAuditLog(newAuditLog);
    }

    public double getNewFilling() {
        return newFilling;
    }
}
