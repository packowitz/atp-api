package io.pacworx.atp.autotrade.domain;

import io.pacworx.atp.autotrade.service.strategies.firstMarket.FirstMarketStrategies;
import io.pacworx.atp.autotrade.service.strategies.firstStepPrice.FirstStepPriceStrategies;
import io.pacworx.atp.autotrade.service.strategies.nextMarket.NextMarketStrategies;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@Entity
public class TradePlanConfig {
    @Id
    private long planId;
    private boolean autoRestart;
    private String startCurrency;
    private double startAmount;
    @Enumerated(EnumType.STRING)
    private FirstMarketStrategies firstMarketStrategy;
    private String firstMarketStrategyParams;
    @Enumerated(EnumType.STRING)
    private FirstStepPriceStrategies firstStepPriceStrategy;
    private String firstStepPriceStrategyParams;
    @Enumerated(EnumType.STRING)
    private NextMarketStrategies nextMarketStrategy;
    private String nextMarketStrategyParams;

    public long getPlanId() {
        return planId;
    }

    public void setPlanId(long planId) {
        this.planId = planId;
    }

    public boolean isAutoRestart() {
        return autoRestart;
    }

    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
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

    public FirstMarketStrategies getFirstMarketStrategy() {
        return firstMarketStrategy;
    }

    public void setFirstMarketStrategy(FirstMarketStrategies firstMarketStrategy) {
        this.firstMarketStrategy = firstMarketStrategy;
    }

    public String getFirstMarketStrategyParams() {
        return firstMarketStrategyParams;
    }

    public void setFirstMarketStrategyParams(String firstMarketStrategyParams) {
        this.firstMarketStrategyParams = firstMarketStrategyParams;
    }

    public FirstStepPriceStrategies getFirstStepPriceStrategy() {
        return firstStepPriceStrategy;
    }

    public void setFirstStepPriceStrategy(FirstStepPriceStrategies firstStepPriceStrategy) {
        this.firstStepPriceStrategy = firstStepPriceStrategy;
    }

    public String getFirstStepPriceStrategyParams() {
        return firstStepPriceStrategyParams;
    }

    public void setFirstStepPriceStrategyParams(String firstStepPriceStrategyParams) {
        this.firstStepPriceStrategyParams = firstStepPriceStrategyParams;
    }

    public NextMarketStrategies getNextMarketStrategy() {
        return nextMarketStrategy;
    }

    public void setNextMarketStrategy(NextMarketStrategies nextMarketStrategy) {
        this.nextMarketStrategy = nextMarketStrategy;
    }

    public String getNextMarketStrategyParams() {
        return nextMarketStrategyParams;
    }

    public void setNextMarketStrategyParams(String nextMarketStrategyParams) {
        this.nextMarketStrategyParams = nextMarketStrategyParams;
    }
}
