package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.TradeOffer;
import io.pacworx.atp.autotrade.domain.binance.BinanceExchangeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class BinanceExchangeInfoService {
    private static final Logger log = LogManager.getLogger();
    private static final String SERVER = "https://api.binance.com/api";

    private Map<String, BinanceExchangeInfo> infos;

    @Scheduled(fixedDelay = 3600000)
    public void loadInfos() {
        RestTemplate restTemplate = new RestTemplate();
        ExchangeInfoResponse response = restTemplate.getForObject(SERVER + "/v1/exchangeInfo", ExchangeInfoResponse.class);
        Map<String, BinanceExchangeInfo> infos = new HashMap<>();
        for(BinanceExchangeInfo info: response.symbols) {
            infos.put(info.getSymbol(), info);
        }
        this.infos = infos;
        log.info("Loaded " + infos.size() + " ExchangeInfos from binance");
    }

    public BinanceExchangeInfo getInfo(String symbol) {
        return this.infos.get(symbol);
    }

    public boolean isTradeBigEnough(String symbol, String currency, double amount, double price) {
        BinanceExchangeInfo info = getInfo(symbol);
        double altCoinAmount;
        double baseCoinAmount;
        // price may vary. include some buffer of 10%
        double calcPrice = price * 0.9;
        if(TradeUtil.isBaseCurrency(currency)) {
            altCoinAmount = amount / calcPrice;
            baseCoinAmount = amount;
        } else {
            altCoinAmount = amount;
            baseCoinAmount = amount * calcPrice;
        }
        return baseCoinAmount > info.getMinBaseAssetQty() && altCoinAmount > info.getQtyStepSize();
    }

    public void polishTradeOffer(TradeOffer offer) {
        BinanceExchangeInfo info = getInfo(offer.getSymbol());
        double polishedAmount = info.getQtyStepSize() * Math.floor(offer.getQuantity() / info.getQtyStepSize());
        offer.setQuantity(polishedAmount);
        double polishedPrice = info.getPriceStepSize() * Math.round(offer.getPrice() / info.getPriceStepSize());
        offer.setPrice(polishedPrice);
    }

    public double polishPrice(String symbol, double price) {
        BinanceExchangeInfo info = getInfo(symbol);
        return info.getPriceStepSize() * Math.round(price / info.getPriceStepSize());
    }

    private static final class ExchangeInfoResponse {
        public String timezone;
        public long serverTime;
        public BinanceExchangeInfo[] symbols;
    }
}
