package io.pacworx.atp.autotrade.service.strategies;

import io.pacworx.atp.autotrade.service.strategies.firstMarket.AnalyzerApiTest;
import io.pacworx.atp.autotrade.service.strategies.firstMarket.FirstMarketStrategies;
import io.pacworx.atp.autotrade.service.strategies.firstMarket.FixedMarket;
import io.pacworx.atp.autotrade.service.strategies.firstMarket.GapAndActive;
import io.pacworx.atp.autotrade.service.strategies.firstStepPrice.DepthPriceAndDistanceFromOtherSide;
import io.pacworx.atp.autotrade.service.strategies.firstStepPrice.FirstStepPriceStrategies;
import io.pacworx.atp.autotrade.service.strategies.firstStepPrice.SimpleDepthPrice;
import io.pacworx.atp.autotrade.service.strategies.nextMarket.DirectBackWithMinProfit;
import io.pacworx.atp.autotrade.service.strategies.nextMarket.NextMarketStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StrategyResolver {

    //FirstStepStrategies
    @Autowired
    private FixedMarket fixedMarket;
    @Autowired
    private GapAndActive gapAndActive;
    @Autowired
    private AnalyzerApiTest analyzerApiTest;

    //FirstStepPriceStrategies
    @Autowired
    private DepthPriceAndDistanceFromOtherSide depthPriceAndDistanceFromOtherSide;
    @Autowired
    private SimpleDepthPrice simpleDepthPrice;

    //NextMarketStrategies
    @Autowired
    private DirectBackWithMinProfit directBackWithMinProfit;

    public MarketStrategy resolveFirstStepStrategy(FirstMarketStrategies strategy) {
        switch (strategy) {
            case FixedMarket: return fixedMarket;
            case GapAndActive: return gapAndActive;
            case AnalyzerApiTest: return analyzerApiTest;
        }
        throw new RuntimeException("No first step strategy found for " + strategy.name());
    }

    public PriceStrategy resolveFirstStepPriceStrategy(FirstStepPriceStrategies strategy) {
        switch (strategy) {
            case DepthPriceAndDistanceFromOtherSide: return depthPriceAndDistanceFromOtherSide;
            case SimpleDepthPrice: return simpleDepthPrice;
        }
        throw new RuntimeException("No first step price strategy found for " + strategy.name());
    }

    public NextMarketStrategy resolveNextStepStrategy(NextMarketStrategies strategy) {
        switch (strategy) {
            case DirectBackWithMinProfit: return directBackWithMinProfit;
        }
        throw new RuntimeException("No next step strategy found for " + strategy.name());
    }
}
