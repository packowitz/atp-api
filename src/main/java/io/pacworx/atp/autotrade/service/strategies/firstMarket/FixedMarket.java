package io.pacworx.atp.autotrade.service.strategies.firstMarket;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.service.strategies.MarketStrategy;
import org.springframework.stereotype.Service;

@Service
public class FixedMarket implements MarketStrategy {

    public String getMarket(TradePlan plan, TradeStep currentStep) {
        return plan.getConfig().getFirstMarketStrategyParams();
    }

    public boolean checkMarket(TradePlan plan, TradeStep currentStep) {
        return false;
    }
}
