package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.TradeOffer;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.domain.TradeStepRepository;
import io.pacworx.atp.autotrade.domain.binance.BinanceDepth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BinanceDepthService {

    private static final Logger log = LogManager.getLogger();
    private static final String SERVER = "https://api.binance.com/api";

    private static final double thresholdPerc = 0.75;

    private final BinanceExchangeInfoService exchangeInfoService;
    private final TradeStepRepository stepRepository;

    public BinanceDepthService(BinanceExchangeInfoService exchangeInfoService,
                               TradeStepRepository stepRepository) {
        this.exchangeInfoService = exchangeInfoService;
        this.stepRepository = stepRepository;
    }

    public BinanceDepth getDepth(String symbol) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(SERVER + "/v1/depth?symbol=" + symbol, BinanceDepth.class);
    }

    public BinanceDepth getDepth(String symbol, DepthLimit limit) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(SERVER + "/v1/depth?symbol=" + symbol + "&limit=" + limit.getLimit(), BinanceDepth.class);
    }

    public double getGoodTradePrice(TradeStep step) {
        double price;
        List<Double> ignorePrices = stepRepository.findActivePrices(step.getSymbol());
        if(TradeUtil.isBuy(step.getSide())) {
            price = getGoodBuyPoint(step.getSymbol(), ignorePrices);
            if(step.getPriceThreshold() != null && price > step.getPriceThreshold()) {
                price = step.getPriceThreshold();
            }
        } else {
            price = getGoodSellPoint(step.getSymbol(), ignorePrices);
            if(step.getPriceThreshold() != null && price < step.getPriceThreshold()) {
                price = step.getPriceThreshold();
            }
        }
        return price;
    }

    private double getGoodBuyPoint(String symbol, List<Double> ignoreBids) {
        BinanceDepth depth = getDepth(symbol, DepthLimit.L20);
        double priceStep = exchangeInfoService.getInfo(symbol).getPriceStepSize();
        double threshold = thresholdPerc / depth.getBids().size();
        List<TradeOffer> overTreshold = depth.getBids().stream().filter(
                t -> !ignoreBids.contains(t.getPrice()) && (t.getQuantity() / depth.getBidVolume()) > threshold).collect(Collectors.toList());
        double priceDiffToHighest = depth.getBids().get(0).getPrice() / overTreshold.get(0).getPrice();
        // if first offer over threshhold is 0.3% away from highest bid use the first offer over threshold
        if(priceDiffToHighest > 1.003) {
            return overTreshold.get(0).getPrice() + priceStep;
        }
        if(overTreshold.size() > 1) {
            //check if 2nd offer is worth to go for
            double priceDiff = overTreshold.get(0).getPrice() / overTreshold.get(1).getPrice();
            if (priceDiff > 1.01) {
                return overTreshold.get(1).getPrice() + priceStep;
            }
        }
        return overTreshold.get(0).getPrice() + priceStep;
    }

    private double getGoodSellPoint(String symbol, List<Double> ignoreAsks) {
        BinanceDepth depth = getDepth(symbol, DepthLimit.L20);
        double priceStep = exchangeInfoService.getInfo(symbol).getPriceStepSize();
        double threshold = thresholdPerc / depth.getAsks().size();
        List<TradeOffer> overTreshold = depth.getAsks().stream().filter(
                t -> !ignoreAsks.contains(t.getPrice()) && (t.getQuantity() / depth.getAskVolume()) > threshold).collect(Collectors.toList());
        double priceDiffToLowest = overTreshold.get(0).getPrice() / depth.getAsks().get(0).getPrice();
        // if first offer over threshhold is 0.3% away from lowest ask use the first offer over threshold
        if(priceDiffToLowest > 1.003) {
            return overTreshold.get(0).getPrice() - priceStep;
        }
        if(overTreshold.size() > 1) {
            //check if 2nd offer is worth to go for
            double priceDiff = overTreshold.get(1).getPrice() / overTreshold.get(0).getPrice();
            if (priceDiff > 1.01) {
                return overTreshold.get(1).getPrice() - priceStep;
            }
        }
        return overTreshold.get(0).getPrice() - priceStep;
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

//    public static void main(String[] args) {
//        BinanceExchangeInfoService exchange = new BinanceExchangeInfoService();
//        exchange.loadInfos();
//        BinanceDepthService service = new BinanceDepthService(exchange);
//
//        String symbol = "BLZBNB";
//        System.out.printf("Good buy: %.8f\n", service.getGoodBuyPoint(symbol));
//        System.out.printf("Good sell: %.8f\n", service.getGoodSellPoint(symbol));
//    }
}
