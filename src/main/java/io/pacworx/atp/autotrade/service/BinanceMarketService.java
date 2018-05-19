package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.binance.BinanceKline;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.autotrade.domain.binance.BinanceTickerStatistics;
import io.pacworx.atp.autotrade.domain.binance.BinanceTrade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BinanceMarketService {
    private static final Logger log = LogManager.getLogger();
    private static final String SERVER = "https://api.binance.com/api";
    private static final long WEEK_IN_MILLIS = 1000 * 60 * 60 * 24 * 7;

    private static final List<String> BLACKLIST = Arrays.asList("BCN");

    private BinanceTicker[] tickerCache;
    private long tickerLoadTimestamp;

    private BinanceTickerStatistics[] statsCache;
    private long statsLoadTimestamp;

    private List<String> newMarketCheckCache = new ArrayList<>();

    public BinanceTicker[] getAllTicker() {
        if(System.currentTimeMillis() - tickerLoadTimestamp < 10000) {
            return tickerCache;
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            BinanceTicker[] tickers = restTemplate.getForObject(SERVER + "/v1/ticker/allBookTickers", BinanceTicker[].class);
            for (BinanceTicker ticker : tickers) {
                double ask = Double.parseDouble(ticker.getAskPrice());
                double bid = Double.parseDouble(ticker.getBidPrice());
                double perc = (ask / bid) - 1;
                ticker.setPerc(perc);
            }

            Arrays.sort(tickers);
            BinanceTickerStatistics[] stats = get24HrPriceStatistics();
            if (stats.length == tickers.length) {
                for (int i = 0; i < tickers.length; i++) {
                    if (tickers[i].getSymbol().equals(stats[i].getSymbol())) {
                        tickers[i].setStats24h(stats[i]);
                    }
                }
            }
            this.tickerCache = tickers;
            this.tickerLoadTimestamp = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Error retrieving ticker from binance", e);
        }
        return tickerCache;
    }

    public BinanceTicker getTicker(String symbol) {
        BinanceTicker[] tickers = getAllTicker();
        for(BinanceTicker ticker: tickers) {
            if(ticker.getSymbol().equals(symbol)) {
                return ticker;
            }
        }
        return null;
    }

    public BinanceTickerStatistics[] get24HrPriceStatistics(){
        if(System.currentTimeMillis() - statsLoadTimestamp < 900000) { // every 15 minutes
            return statsCache;
        }
        RestTemplate restTemplate = new RestTemplate();
        BinanceTickerStatistics[] stats  = restTemplate.getForObject(SERVER + "/v1/ticker/24hr", BinanceTickerStatistics[].class);
        Arrays.sort(stats);
        this.statsCache = stats;
        this.statsLoadTimestamp = System.currentTimeMillis();
        return statsCache;
    }

    public BinanceTrade[] getLastTrades(String symbol, int limit) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(SERVER + "/v1/trades?symbol=" + symbol + "&limit=" + limit, BinanceTrade[].class);
    }

    public boolean isMarketOldEnough(String symbol) {
        boolean oldEnough = newMarketCheckCache.contains(symbol);
        if(!oldEnough) {
            RestTemplate restTemplate = new RestTemplate();
            BinanceKline[] klines = restTemplate.getForObject(SERVER + "/v1/klines?symbol=" + symbol + "&interval=1d", BinanceKline[].class);
            if(klines != null && klines.length > 0) {
                BinanceKline firstEntry = klines[0];
                if((System.currentTimeMillis() - WEEK_IN_MILLIS) > firstEntry.getOpenTime()) {
                    oldEnough = true;
                    newMarketCheckCache.add(symbol);
                }
            }
        }
        return oldEnough;
    }

    public boolean isBlacklisted(String symbol) {
        String altCoin = TradeUtil.getAltCoin(symbol);
        return BLACKLIST.contains(altCoin);
    }
}
