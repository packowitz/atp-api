package io.pacworx.atp.autotrade.service.strategies.firstStepPrice;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.autotrade.service.BinanceDepthService;
import io.pacworx.atp.autotrade.service.BinanceService;
import io.pacworx.atp.autotrade.service.TradeUtil;
import io.pacworx.atp.autotrade.service.strategies.PriceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DepthPriceAndDistanceFromOtherSide implements PriceStrategy {

    @Autowired
    BinanceDepthService depthService;

    @Autowired
    BinanceService binanceService;

    public double getPrice(TradePlan plan, TradeStep step) {
        return depthService.getGoodTradePrice(step);
    }

    public boolean isThresholdDynamic() {
        return true;
    }

    public void setThresholdToStep(TradePlan plan, TradeStep step, TradeStep prevStep) {
        //Set step's threshold to configured distance from other side
        BinanceTicker ticker = binanceService.getTicker(step.getSymbol());
        double distance = Double.parseDouble(plan.getConfig().getFirstStepPriceStrategyParams());
        double threshold;
        if(TradeUtil.isBuy(step.getSide())) {
            threshold = Double.parseDouble(ticker.getAskPrice()) / (1d + distance);
        } else {
            threshold = Double.parseDouble(ticker.getBidPrice()) * (1d + distance);
        }
        step.setPriceThreshold(threshold);
    }
}
