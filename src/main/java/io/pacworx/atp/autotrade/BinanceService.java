package io.pacworx.atp.autotrade;

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

@Component
public class BinanceService {
    private static final Logger log = LogManager.getLogger();
    private static final String SERVER = "https://api.binance.com/api";

    private final BinanceExchangeInfoService exchangeInfoService;
    private long serverTimeDifference = 0;

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

    public Ticker[] getAllTicker() {
        RestTemplate restTemplate = new RestTemplate();
        Ticker[] tickers = restTemplate.getForObject(SERVER + "/v1/ticker/allBookTickers", Ticker[].class);
        for(Ticker ticker : tickers) {
            double ask = Double.parseDouble(ticker.getAskPrice());
            double bid = Double.parseDouble(ticker.getBidPrice());
            double perc = (ask / bid) - 1;
            ticker.setPerc(perc);
        }
        return tickers;
    }

    public Depth getDepth(String symbol) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(SERVER + "/v1/depth?symbol=" + symbol, Depth.class);
    }

    public BinanceAccount getBinanceAccount(TradeAccount account) {
        return doSignedGet("/v3/account", null, account, BinanceAccount.class);
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

    private <T>T doSignedPost(String path, String params, TradeAccount account, Class<T> returnClass) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER + path;
        String body = signParams(account, params);

        HttpEntity<String> entity = new HttpEntity<>(body, getHeaders(account));

        ResponseEntity<T> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST,  entity, returnClass);
        } catch (HttpClientErrorException e) {
            log.error("Post to binance ended with " + e.getStatusCode() + " and body: " + e.getResponseBodyAsString());
            throw new BadRequestException();
        }
        return response.getBody();
    }

    private <T>T doSignedDelete(String path, String params, TradeAccount account, Class<T> returnClass) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER + path + "?" + signParams(account, params);

        HttpEntity<String> entity = new HttpEntity<>(getHeaders(account));

        ResponseEntity<T> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.DELETE,  entity, returnClass);
        } catch (HttpClientErrorException e) {
            log.error("Delete to binance ended with " + e.getStatusCode() + " and body: " + e.getResponseBodyAsString());
            throw new BadRequestException();
        }
        return response.getBody();
    }

    private <T>T doSignedGet(String path, String params, TradeAccount account, Class<T> returnClass) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER + path + "?" + signParams(account, params);

        HttpEntity<String> entity = new HttpEntity<>(getHeaders(account));

        ResponseEntity<T> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET,  entity, returnClass);
        } catch (HttpClientErrorException e) {
            log.error("Get to binance ended with " + e.getStatusCode() + " and body: " + e.getResponseBodyAsString());
            throw new BadRequestException();
        }
        return response.getBody();
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
}
