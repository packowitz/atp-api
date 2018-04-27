package io.pacworx.atp.autotrade.service.strategies;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradeStep;

public interface PriceStrategy {

    double getPrice(TradePlan plan, TradeStep step);

    void setThresholdToStep(TradePlan plan, TradeStep step, TradeStep prevStep);
}
