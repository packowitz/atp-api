package io.pacworx.atp.autotrade.service.strategies.firstMarket;

import io.pacworx.atp.autotrade.domain.TradePlan;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.autotrade.domain.binance.BinanceTrade;
import io.pacworx.atp.autotrade.service.BinanceMarketService;
import io.pacworx.atp.autotrade.service.TradeUtil;
import io.pacworx.atp.autotrade.service.strategies.MarketStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GapAndActive implements MarketStrategy {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private BinanceMarketService marketService;

    private static final long halfHourInMillies = 30 * 60 * 1000;
    private static final double MAX_GAP = 0.02;

    public boolean checkMarket(TradePlan plan, TradeStep currentStep) {
        if(currentStep.getCheckedMarketDate() == null) {
            return true;
        }
        //check at least every minute
        if(currentStep.getCheckedMarketDate().plusMinutes(1).isBefore(ZonedDateTime.now())) {
            return true;
        }
        //check in chase gap falls under the configured minimum or exceeds the max gap
        if(currentStep.getSymbol() != null) {
            BinanceTicker ticker = marketService.getTicker(currentStep.getSymbol());
            double minGap = Double.parseDouble(plan.getConfig().getFirstMarketStrategyParams());
            return ticker.getPerc() < minGap || ticker.getPerc() > MAX_GAP;
        }
        return false;
    }

    public String getMarket(TradePlan plan, TradeStep currentStep) {
        String currency = plan.getConfig().getStartCurrency();
        double minGap = Double.parseDouble(plan.getConfig().getFirstMarketStrategyParams());
        BinanceTicker[] ticker = marketService.getAllTicker();

        List<BinanceTicker> possibleMarkets;
        if(TradeUtil.isBaseCurrency(currency)) {
            possibleMarkets = Arrays.stream(ticker).filter(t -> t.getPerc() >= minGap && t.getPerc() < MAX_GAP && t.getSymbol().endsWith(currency)).collect(Collectors.toList());
        } else {
            possibleMarkets = Arrays.stream(ticker).filter(t -> {
                if(t.getPerc() >= minGap && t.getPerc() < MAX_GAP && t.getSymbol().startsWith(currency)) {
                    return TradeUtil.isBaseCurrency(t.getSymbol().substring(currency.length()));
                }
                return false;
            }).collect(Collectors.toList());
        }

        if(!possibleMarkets.isEmpty()) {
            possibleMarkets = possibleMarkets.stream().filter(this::isMarketHealthy).collect(Collectors.toList());
        }
        if(possibleMarkets.size() > 1) {
            possibleMarkets = possibleMarkets.stream().sorted(Comparator.comparing(BinanceTicker::getPerc).reversed()).collect(Collectors.toList());
        }

        String bestMarket = null;
        double bestMarketScore = 0d;
        int maxChecks = 10;
        for(BinanceTicker tick: possibleMarkets) {
            if(maxChecks == 0 || bestMarketScore == 10000) {
                break;
            }
            maxChecks --;
            double score = calcScore(tick, TradeUtil.isBuy(tick.getSymbol(), currency));
            //log.info(tick.getSymbol() + " scored " + score + " with ticker gap: " + String.format("%.2f", 100d * tick.getPerc()) + "%");
            if(score > bestMarketScore) {
                bestMarket = tick.getSymbol();
                bestMarketScore = score;
            }
        }
        if(bestMarket != null) {
            log.info("Best market for trading " + currency + " is " + bestMarket + " with a score of " + bestMarketScore);
        } else {
            log.info("No good market found to trade " + currency);
        }
        return bestMarket;
    }

    private boolean isMarketHealthy(BinanceTicker ticker) {
        if(marketService.isBlacklisted(ticker.getSymbol()) || ticker.getStats24h() == null) {
            return false;
        }
        //market need to have at least 2 trades in avg in last 24h
        if(ticker.getStats24h().getCount() < 2880) {
            return false;
        }
        //market should not be in lower or upper 20% of low/high mark of last 24h
        double range24h = Double.parseDouble(ticker.getStats24h().getHighPrice()) - Double.parseDouble(ticker.getStats24h().getLowPrice());
        double bidRatio = (Double.parseDouble(ticker.getBidPrice()) - Double.parseDouble(ticker.getStats24h().getLowPrice())) / range24h;
        double askRatio = (Double.parseDouble(ticker.getAskPrice()) - Double.parseDouble(ticker.getStats24h().getLowPrice())) / range24h;
        if(range24h <= 0 || bidRatio <= 0.2 || askRatio >= 0.8) {
            //System.out.println(ticker.getSymbol() + " excluded: (H " + ticker.getStats24h().getHighPrice() + "  L " + ticker.getStats24h().getLowPrice() + ") " + String.format("%.8f", range24h) + " -> B(" + ticker.getBidPrice() + ") " + bidRatio + "  A(" + ticker.getAskPrice() + ") " + askRatio);
            return false;
        }
        //avoid new markets
        if(!marketService.isMarketOldEnough(ticker.getSymbol())) {
            return false;
        }
        return true;
    }

    private double calcScore(BinanceTicker tick, boolean isBuy) {
        // Get last 200 trades
        BinanceTrade[] last200trades = marketService.getLastTrades(tick.getSymbol(), 200);
        long timestamp = System.currentTimeMillis() - halfHourInMillies;

        //Activity check
        List<BinanceTrade> tradesLast30min = Arrays.stream(last200trades).filter(t -> t.getTime() > timestamp).collect(Collectors.toList());
        if(tradesLast30min.size() < 60) {
            return -1;
        }
        int activityScore = 50;
        activityScore += (tradesLast30min.size() - 60) / 2;
        if(activityScore > 100) {
            activityScore = 100;
        }

        //Volume and trend check
        List<BinanceTrade> last30trades = tradesLast30min.subList(Math.max(0, tradesLast30min.size() - 30), tradesLast30min.size());
        int buys = 0, sells = 0, trend = 0;
        double sellVolume = 0d, buyVolume = 0d;
        Double lastTradePrice = null;
        for (BinanceTrade trade : last30trades) {
            if (trade.getIsBuyerMaker()) {
                sells++;
                sellVolume += trade.getQty();
                //check trend to go up
                if(isBuy) {
                    if(lastTradePrice != null) {
                        if(trade.getPrice() > lastTradePrice) {
                            trend ++;
                        } else if(trade.getPrice() < lastTradePrice) {
                            trend --;
                        }
                    }
                    lastTradePrice = trade.getPrice();
                }
            } else {
                buys++;
                buyVolume += trade.getQty();
                //check trend to go down
                if(!isBuy) {
                    if(lastTradePrice != null) {
                        if(trade.getPrice() < lastTradePrice) {
                            trend ++;
                        } else if(trade.getPrice() > lastTradePrice) {
                            trend --;
                        }
                    }
                    lastTradePrice = trade.getPrice();
                }
            }
        }
        System.out.println(tick.getSymbol() + " has trend of " + trend);
        if(buys < 5 || sells < 5 || trend <= 0) {
            return -1;
        }
        double ratio = Math.abs((buyVolume / (sellVolume + buyVolume)) - 0.5); //0 best, 0.5 worst
        if(ratio > 0.25) {
            return -1;
        }

        int volScore = 100;
        if(ratio >= 0.1) {
            volScore -= (int)Math.round((ratio - 0.1) * 200);
        }

        return activityScore * volScore;
    }

//    public static void main(String[] args) {
//        BinanceMarketService marketService = new BinanceMarketService();
//        GapAndActive service = new GapAndActive();
//        service.marketService = marketService;
//
//        TradePlanConfig config = new TradePlanConfig();
//        config.setStartCurrency("BTC");
//        config.setFirstMarketStrategyParams("0.003");
//        TradePlan plan = new TradePlan();
//        plan.setConfig(config);
//
//        long time = System.currentTimeMillis();
//        System.out.println(service.getMarket(plan, null));
//        System.out.println("Calculation took " + (System.currentTimeMillis() - time) + "ms");
//    }
}
