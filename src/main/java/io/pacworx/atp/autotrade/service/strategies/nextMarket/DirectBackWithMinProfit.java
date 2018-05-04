package io.pacworx.atp.autotrade.service.strategies.nextMarket;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.service.BinanceDepthService;
import io.pacworx.atp.autotrade.service.BinanceExchangeInfoService;
import io.pacworx.atp.autotrade.service.TradeUtil;
import io.pacworx.atp.autotrade.service.strategies.NextMarketStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DirectBackWithMinProfit implements NextMarketStrategy {

    @Autowired
    private BinanceDepthService depthService;
    @Autowired
    private BinanceExchangeInfoService exchangeInfoService;

    public String getMarket(TradePlan plan, TradeStep currentStep) {
        return currentStep.getSymbol();
    }

    public boolean checkMarket(TradePlan plan, TradeStep currentStep) {
        return false;
    }

    public boolean allowPartialNextStep() {
        return true;
    }

    public double getPrice(TradePlan plan, TradeStep step) {
        return depthService.getGoodTradePrice(step);
    }

    public void setThresholdToStep(TradePlan plan, TradeStep step, TradeStep prevStep) {
        double minProfit = Double.parseDouble(plan.getConfig().getNextMarketStrategyParams());
        double threshold = calcPriceThreshold(step, prevStep.getPrice(), minProfit);
        if(step.getPriceThreshold() != null) {
            //need to merge threshold with existing one
            double newAmount = prevStep.getOutAmount() - step.getInAmount();
            double newThreshold = threshold;
            double oldAmount = step.getInAmount() - step.getInFilled();
            double oldThreshold = step.getPriceThreshold();

            threshold = avgPriceThreshold(step.getSymbol(), newAmount, newThreshold, oldAmount, oldThreshold);
        }
        step.setPriceThreshold(threshold);
    }

    private double calcPriceThreshold(TradeStep step, double previousPrice, double minProfit) {
        double price = previousPrice;
        if(TradeUtil.isBuy(step.getSide())) {
            // buy it minProfit lower than sold
            price /= (1d + minProfit);
        } else {
            // sell it minProfit higher than bought
            price *= (1d + minProfit);
        }
        return exchangeInfoService.polishPrice(step.getSymbol(), price);
    }

    private double avgPriceThreshold(String symbol, double amount1, double threshold1, double amount2, double threshold2) {
        double avg = ((amount1 * threshold1) + (amount2 * threshold2)) / (amount1 + amount2);
        return exchangeInfoService.polishPrice(symbol, avg);
    }
}
