package io.pacworx.atp.autotrade.service.strategies.firstMarket;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.autotrade.service.BinanceMarketService;
import io.pacworx.atp.autotrade.service.strategies.MarketStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AnalyzerApiTest implements MarketStrategy {

    private static final Logger log = LogManager.getLogger();

    @Value("${analyer.api.url}")
    private String analyzerApiUrl;

    @Autowired
    private BinanceMarketService marketService;

    public String getMarket(TradePlan plan, TradeStep currentStep) {
        String url = analyzerApiUrl + "/" + plan.getConfig().getStartCurrency();
        HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());

        ResponseEntity<AnalyzerApiScore[]> response;
        RestTemplate restTemplate = new RestTemplate();
        try {
            response = restTemplate.exchange(url, HttpMethod.GET,  entity, AnalyzerApiScore[].class);
        } catch (HttpClientErrorException e) {
            log.error(e.getLocalizedMessage(), e);
            return null;
        }
        AnalyzerApiScore[] scores = response.getBody();
        double minGap = Double.parseDouble(plan.getConfig().getFirstMarketStrategyParams());
        for(AnalyzerApiScore score: scores) {
            if(!marketService.isBlacklisted(score.symbol) && marketService.isMarketOldEnough(score.symbol)) {
                BinanceTicker ticker = marketService.getTicker(score.symbol);
                if(ticker != null && ticker.getPerc() >= minGap) {
                    return score.symbol;
                }
            }
        }
        return null;
    }

    public boolean checkMarket(TradePlan plan, TradeStep currentStep) {
        return true;
    }

    private static final class AnalyzerApiScore {
        public String symbol;
        public int period;
        public int barCount;
        public double score;
    }
}
