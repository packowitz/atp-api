package io.pacworx.atp.autotrade.service.strategies;

import io.pacworx.atp.autotrade.service.strategies.MarketStrategy;
import io.pacworx.atp.autotrade.service.strategies.PriceStrategy;

public interface NextMarketStrategy extends MarketStrategy, PriceStrategy {

    boolean allowPartialNextStep();
}
