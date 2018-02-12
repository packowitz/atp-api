package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.TradeOffer;
import io.pacworx.atp.autotrade.domain.binance.BinanceDepth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BinanceDepthService {

    private static final Logger log = LogManager.getLogger();
    private static final String SERVER = "https://api.binance.com/api";

    private final BinanceExchangeInfoService exchangeInfoService;

    public BinanceDepthService(BinanceExchangeInfoService exchangeInfoService) {
        this.exchangeInfoService = exchangeInfoService;
    }

    public BinanceDepth getDepth(String symbol) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(SERVER + "/v1/depth?symbol=" + symbol, BinanceDepth.class);
    }

    public BinanceDepth getDepth(String symbol, DepthLimit limit) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(SERVER + "/v1/depth?symbol=" + symbol + "&limit=" + limit.getLimit(), BinanceDepth.class);
    }

    public double getGoodBuyPoint(String symbol) {
        BinanceDepth depth = getDepth(symbol, DepthLimit.L20);
        double treshold = 100d / depth.getBids().size();
        for(TradeOffer bid: depth.getBids()) {
            if(treshold < 100d * bid.getQuantity() / depth.getBidVolume()) {
                return bid.getPrice() + exchangeInfoService.getInfo(symbol).getPriceStepSize();
            }
        }
        throw new RuntimeException("Found no good buy point for " + symbol);
    }

    public double getGoodSellPoint(String symbol) {
        BinanceDepth depth = getDepth(symbol, DepthLimit.L20);
        double treshold = 100d / depth.getAsks().size();
        for(TradeOffer ask: depth.getAsks()) {
            double perc = 100d * ask.getQuantity() / depth.getAskVolume();
            if(perc > treshold) {
                return ask.getPrice() - exchangeInfoService.getInfo(symbol).getPriceStepSize();
            }
        }
        throw new RuntimeException("Found no good sell point for " + symbol);
    }

    public enum DepthLimit {
        L5(5), L10(10), L20(20), L50(50), L100(100);

        int limit;

        DepthLimit(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }
    }
}
