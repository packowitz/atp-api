package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RouteCalculator {
    private int maxSteps;
    private String startCur;
    private double startAmount;
    private String destCur;
    private List<BinanceTicker> tickers;
    private Route bestRoute;

    public RouteCalculator(int maxSteps, String startCur, double startAmount, String destCur, List<BinanceTicker> tickers) {
        this.maxSteps = maxSteps;
        this.startCur = startCur;
        this.startAmount = startAmount;
        this.destCur = destCur;
        this.tickers = tickers;
    }

    public Route searchBestRoute() {
        Route route = new Route(startCur, startAmount, destCur);
        nextSteps(route);
        return bestRoute;
    }

    private void nextSteps(Route route) {
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

    static final class Route {
        String startCur;
        double startAmount;
        String destCur;
        String lastCur;
        List<RouteStep> steps = new ArrayList<>();

        double finalAmount;

        Route(String startCur, double startAmount, String destCur) {
            this.startCur = startCur;
            this.startAmount = startAmount;
            this.lastCur = startCur;
            this.destCur = destCur;
        }

        Route(Route orig) {
            this(orig.startCur, orig.startAmount, orig.destCur);
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
            double tradeAmount = startAmount;
            for(RouteStep step: steps) {
                step.calc(tradeCur, tradeAmount);
                tradeCur = step.cur;
                tradeAmount = step.amount;
            }
            finalAmount = tradeAmount;
        }
    }

    static final class RouteStep {
        boolean isBuy;
        BinanceTicker ticker;
        /** Identifier where in the gap between highest bid and lowest ask to place the bid **/
        double tradePerc = 1;
        double tradePoint;
        String cur;
        double amount;

        RouteStep(boolean isBuy, BinanceTicker ticker) {
            this.isBuy = isBuy;
            this.ticker = ticker;
            this.adjustTradePercByActivity();
        }

        private void adjustTradePercByActivity() {
            //check the last24h stats (1440 minutes)
            long trades = this.ticker.getStats24h().getCount();
            //over 2 trades per minute -> active market
            //over 1 trade per minute -> slightly inactive market
            //over 0.5 trades per minute -> inactive market
            //less than 0.5 trades per minute -> avoid market
            if(trades >= 2880) {
                tradePerc = 1;
            } else if(trades >= 1440) {
                tradePerc = 0.8;
            } else if(trades >= 720) {
                tradePerc = 0.5;
            } else {
                tradePerc = 0;
            }
        }

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
