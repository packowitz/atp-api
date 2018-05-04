package io.pacworx.atp.autotrade.service.strategies;

public interface NextMarketStrategy extends MarketStrategy, PriceStrategy {

    boolean allowPartialNextStep();
}
