package io.pacworx.atp.autotrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pacworx.atp.autotrade.domain.TradeAccount;
import io.pacworx.atp.autotrade.domain.TradeOffer;
import io.pacworx.atp.autotrade.domain.TradeStatus;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.domain.binance.*;
import io.pacworx.atp.exception.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.ZonedDateTime;
import java.util.Arrays;

@Component
public class BinanceService {
    private static final Logger log = LogManager.getLogger();
    private static final String SERVER = "https://api.binance.com/api";

    private final BinanceExchangeInfoService exchangeInfoService;
    private long serverTimeDifference = 0;

    private BinanceTicker[] tickerCache;
    private long tickerLoadTimestamp;
    
    private BinanceTickerStatistics[] statsCache;
    private long statsLoadTimestamp;

    @Autowired
    public BinanceService(BinanceExchangeInfoService exchangeInfoService) {
        this.exchangeInfoService = exchangeInfoService;
    }

    @Scheduled(fixedDelay = 120000)
    public void calcServerTimeDifference() {
        long startTime = System.currentTimeMillis();
        RestTemplate restTemplate = new RestTemplate();
        ServerTimeResponse response = restTemplate.getForObject(SERVER + "/v1/time", ServerTimeResponse.class);
        long endTime = System.currentTimeMillis();
        long middle = startTime + ((endTime - startTime) / 2);
        this.serverTimeDifference = response.serverTime - middle;
        log.info("Found a time difference to binance of " + serverTimeDifference + "ms.");
    }

    public BinanceTicker[] getAllTicker() {
        if(System.currentTimeMillis() - tickerLoadTimestamp < 10000) {
            return tickerCache;
        }
        RestTemplate restTemplate = new RestTemplate();
        BinanceTicker[] tickers = restTemplate.getForObject(SERVER + "/v1/ticker/allBookTickers", BinanceTicker[].class);
        for(BinanceTicker ticker : tickers) {
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
        return tickerCache;
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

    public BinanceTrade[] getLastTrades(String symbol) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(SERVER + "/v1/trades?symbol=" + symbol, BinanceTrade[].class);
    }

    public BinanceAccount getBinanceAccount(TradeAccount account) {
        return doSignedGet("/v3/account", null, account, BinanceAccount.class);
    }

    public BinanceOrderResult openStepOrder(TradeAccount account, TradeStep step) {
        double amount = step.getInAmount() - step.getInFilled();
        if(TradeUtil.isBuy(step.getSide())) {
            amount /= step.getPrice();
        }

        TradeOffer offer = new TradeOffer(step.getSymbol(), step.getSide().toUpperCase(), step.getPrice(), amount);
        BinanceOrderResult result = openLimitOrder(account, offer);

        if(step.getStartDate() == null) {
            step.setStartDate(ZonedDateTime.now());
        }
        step.setOrderId(result.getOrderId());
        step.setOrderFilled(0d);
        step.setOrderAltcoinQty(Double.parseDouble(result.getOrigQty()));
        step.setOrderBasecoinQty(step.getOrderAltcoinQty() * Double.parseDouble(result.getPrice()));
        step.setStatus(TradeStatus.ACTIVE);
        step.setDirty();

        String logMsg = "Opened order " + result.getOrderId() + " for plan #" + step.getPlanId() + "-" + step.getStep() + ": ";
        if(TradeUtil.isBuy(result.getSide())) {
            logMsg += "BUY " + step.getOrderAltcoinQty() + " " + step.getOutCurrency() + " for ";
            logMsg += step.getOrderBasecoinQty() + " " + step.getInCurrency();
        } else {
            logMsg += "SELL " + String.format("%.8f", step.getOrderAltcoinQty()) + " " + step.getInCurrency() + " to ";
            logMsg += String.format("%.8f", step.getOrderBasecoinQty()) + " " + step.getOutCurrency();
        }
        logMsg += " at " + String.format("%.8f", step.getPrice());
        log.info(logMsg);
        return result;
    }

    public BinanceOrderResult openLimitOrder(TradeAccount account, TradeOffer offer) {
        this.exchangeInfoService.polishTradeOffer(offer);
        String params = "symbol=" + offer.getSymbol();
        params += "&side=" + offer.getSide();
        params += "&type=LIMIT";
        params += "&timeInForce=GTC";
        params += "&quantity=" + String.format("%.8f", offer.getQuantity());
        params += "&price=" + String.format("%.8f", offer.getPrice());
        log.info("Open order: " + offer.getSide() + " " + String.format("%.8f", offer.getQuantity()) + " " + offer.getSymbol() + " at " + String.format("%.8f", offer.getPrice()));
        return doSignedPost("/v3/order", params, account, BinanceOrderResult.class);
    }

    public BinanceOrderResult getOrderStatus(TradeAccount account, String symbol, long orderId) {
        String params = "symbol=" + symbol;
        params += "&orderId=" + orderId;
        return doSignedGet("/v3/order", params, account, BinanceOrderResult.class);
    }

    public BinanceOrderResult cancelOrder(TradeAccount account, String symbol, long orderId) {
        String params = "symbol=" + symbol;
        params += "&orderId=" + orderId;
        log.info("Cancel order " + orderId + " (" + symbol + ")");
        return doSignedDelete("/v3/order", params, account, BinanceOrderResult.class);
    }

    public BinanceOrderResult cancelStep(TradeAccount account, TradeStep step) {
        try {
            cancelOrder(account, step.getSymbol(), step.getOrderId());
        } catch(Exception e) {
            // this happens when the order is already filled or canceled
            // TODO add check it the exception is caused by this
        }
        step.setStatus(TradeStatus.CANCELLED);
        step.setDirty();
        return getOrderStatus(account, step.getSymbol(), step.getOrderId());
    }

    private <T>T doSignedPost(String path, String params, TradeAccount account, Class<T> returnClass) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER + path;
        String body = signParams(account, params);

        HttpEntity<String> entity = new HttpEntity<>(body, getHeaders(account));

        ResponseEntity<T> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST,  entity, returnClass);
        } catch (HttpClientErrorException e) {
            handleBinanceError(e);
        }
        return response.getBody();
    }

    private <T>T doSignedDelete(String path, String params, TradeAccount account, Class<T> returnClass) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER + path + "?" + signParams(account, params);

        HttpEntity<String> entity = new HttpEntity<>(getHeaders(account));

        ResponseEntity<T> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE,  entity, returnClass);
        } catch (HttpClientErrorException e) {
            handleBinanceError(e);
        }
        return response.getBody();
    }

    private <T>T doSignedGet(String path, String params, TradeAccount account, Class<T> returnClass) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER + path + "?" + signParams(account, params);

        HttpEntity<String> entity = new HttpEntity<>(getHeaders(account));

        ResponseEntity<T> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET,  entity, returnClass);
        } catch (HttpClientErrorException e) {
            handleBinanceError(e);
        }
        return response.getBody();
    }

    private void handleBinanceError(HttpClientErrorException e) {
        try {
            BinanceErrorResponse errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), BinanceErrorResponse.class);
            log.error("binance request ended with " + e.getStatusCode() + ", code " + errorResponse.code + " and msg " + errorResponse.msg);
            if(errorResponse.code == -1021) {
                calcServerTimeDifference();
            }
        } catch (Exception ex) {
            log.error("Not able to parse binance error: " + e.getResponseBodyAsString());
        }
        throw new BadRequestException();
    }

    private String signParams(TradeAccount account, String params) {
        if(params == null || params.length() == 0) {
            params = "";
        } else {
            params += "&";
        }
        params += "recvWindow=5000&timestamp=" + (System.currentTimeMillis() + serverTimeDifference);
        params += "&signature=" + getSignature(params, account.getPrivateKeyUnencrypted());
        return params;
    }

    private HttpHeaders getHeaders(TradeAccount account) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", account.getApiKeyUnencrypted());
        return headers;
    }

    private String getSignature(String message, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            final byte[] mac_data = sha256_HMAC.doFinal(message.getBytes());
            String result = "";
            for (final byte element : mac_data)
            {
                result += Integer.toString((element & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static final class ServerTimeResponse {
        public long serverTime;
    }

    private static final class BinanceErrorResponse {
        public int code;
        public String msg;
    }
}
