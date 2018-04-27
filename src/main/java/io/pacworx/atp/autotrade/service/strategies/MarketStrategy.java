package io.pacworx.atp.autotrade.service.strategies;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradePlanConfig;
import io.pacworx.atp.autotrade.domain.TradeStep;

public interface MarketStrategy {
    String getMarket(TradePlan plan, TradeStep currentStep);
}
