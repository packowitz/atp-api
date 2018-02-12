package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RouteCalculator {
    int maxSteps;
    String startCur;
    String destCur;
    List<BinanceTicker> tickers;
    Route bestRoute;

    public RouteCalculator(int maxSteps, String startCur, String destCur, List<BinanceTicker> tickers) {
        this.maxSteps = maxSteps;
        this.startCur = startCur;
        this.destCur = destCur;
        this.tickers = tickers;
    }

    public Route searchBestRoute() {
        Route route = new Route(startCur, destCur);
        nextSteps(route);
        return bestRoute;
    }

    void nextSteps(Route route) {
        List<BinanceTicker> buys = this.findBuys(route.lastCur);
        for(BinanceTicker ticker: buys) {
            addStepToRoute(route, new RouteStep(true, ticker));
        }
        List<BinanceTicker> sells = this.findSells(route.lastCur);
        for(BinanceTicker ticker: sells) {
            addStepToRoute(route, new RouteStep(false, ticker));
        }
    }

    private void addStepToRoute(Route origRoute, RouteStep step) {
        Route route = new Route(origRoute);
        route.addStep(step);
        if(route.isRouteFinished()) {
            calcBetterRoute(route);
        } else {
            if(route.steps.size() < maxSteps) {
                nextSteps(route);
            }
        }
    }

    private void calcBetterRoute(Route newRoute) {
        newRoute.calc();
        if(bestRoute == null || newRoute.finalAmount > bestRoute.finalAmount) {
            bestRoute = newRoute;
        }
    }

    private List<BinanceTicker> findBuys(String cur) {
        if(!TradeUtil.isBaseCurrency(cur)) {
            return Collections.emptyList();
        }
        return tickers.stream().filter(t -> t.getSymbol().endsWith(cur)).collect(Collectors.toList());
    }

    private List<BinanceTicker> findSells(String cur) {
        return tickers.stream().filter(t -> {
            if(t.getSymbol().startsWith(cur)) {
                String otherCur = TradeUtil.otherCur(t.getSymbol(), cur);
                return TradeUtil.isBaseCurrency(otherCur);
            } else {
                return false;
            }
        }).collect(Collectors.toList());
    }

    public static final class Route {
        String startCur;
        String destCur;
        String lastCur;
        List<RouteStep> steps = new ArrayList<>();

        double finalAmount;

        public Route(String startCur, String destCur) {
            this.startCur = startCur;
            this.lastCur = startCur;
            this.destCur = destCur;
        }

        public Route(Route orig) {
            this(orig.startCur, orig.destCur);
            lastCur = orig.lastCur;
            steps = new ArrayList<>(orig.steps);
        }

        void addStep(RouteStep step) {
            lastCur = TradeUtil.otherCur(step.ticker.getSymbol(), lastCur);
            steps.add(step);
        }

        boolean isRouteFinished() {
            if(startCur.equals(destCur)) {
                return destCur.equals(lastCur) && steps.size() > 1;
            } else {
                return destCur.equals(lastCur);
            }
        }

        void calc() {
            String tradeCur = startCur;
            double tradeAmount = 1d;
            for(RouteStep step: steps) {
                step.calc(tradeCur, tradeAmount);
                tradeCur = step.cur;
                tradeAmount = step.amount;
            }
            finalAmount = tradeAmount;
        }
    }

    public static final class RouteStep {
        boolean isBuy;
        BinanceTicker ticker;
        double tradePerc = 0.5;
        double tradePoint;
        String cur;
        double amount;

        public RouteStep(boolean isBuy, BinanceTicker ticker) {
            this.isBuy = isBuy;
            this.ticker = ticker;
            //this.optimizeTradePerc();
        }

//        private void optimizeTradePerc() {
//            if(ticker.getPerc() < 0.002) {
//                tradePerc = 0;
//            } else if(ticker.getPerc() < 0.005) {
//                tradePerc = 0.5;
//            } else {
//                tradePerc = 0.9;
//            }
//        }

        void calc(String inCur, double inAmount) {
            cur = TradeUtil.otherCur(ticker.getSymbol(), inCur);
            double ask = Double.parseDouble(ticker.getAskPrice());
            double bid = Double.parseDouble(ticker.getBidPrice());
            double buyAskDiff = ask - bid;
            int precission = 1;
            while(precission * bid < 1) {
                precission *= 10;
            }
            precission *= 1000;
            if(isBuy) {
                tradePoint = ask - (buyAskDiff * tradePerc);
                tradePoint = Math.round(precission * tradePoint);
                tradePoint /= precission;
                amount = inAmount / tradePoint;
            } else {
                tradePoint = bid + (buyAskDiff * tradePerc);
                tradePoint = Math.round(precission * tradePoint);
                tradePoint /= precission;
                amount = inAmount * tradePoint;
            }
        }
    }
}
