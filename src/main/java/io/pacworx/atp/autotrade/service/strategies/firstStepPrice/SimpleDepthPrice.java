package io.pacworx.atp.autotrade.service.strategies.firstStepPrice;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.service.BinanceDepthService;
import io.pacworx.atp.autotrade.service.strategies.PriceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SimpleDepthPrice implements PriceStrategy {

    @Autowired
    BinanceDepthService depthService;

    public double getPrice(TradePlan plan, TradeStep step) {
        return depthService.getGoodTradePrice(step);
    }

    public boolean isThresholdDynamic() {
        return false;
    }

    public void setThresholdToStep(TradePlan plan, TradeStep step, TradeStep prevStep) {
        step.setPriceThreshold(null);
    }
}
