package io.pacworx.atp.autotrade.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pacworx.atp.autotrade.domain.TradeAccount;
import io.pacworx.atp.autotrade.domain.TradeOffer;
import io.pacworx.atp.autotrade.domain.TradeStatus;
import io.pacworx.atp.autotrade.domain.TradeStep;
import io.pacworx.atp.autotrade.domain.binance.BinanceAccount;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.autotrade.domain.binance.BinanceTrade;
import io.pacworx.atp.exception.BinanceException;
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

@Component
public class BinanceOrderService {
    private static final Logger log = LogManager.getLogger();
    private static final String SERVER = "https://api.binance.com/api";

    public static final int ERROR_CODE_TIME_DIFF = -1021;
    public static final int ERROR_CODE_UNKNOWN_ORDER = -2011;
    public static final int ERROR_CODE_ORDER_NOT_EXIST = -2013;

    @Autowired
    private BinanceExchangeInfoService exchangeInfoService;
    @Autowired
    private BinanceMarketService marketService;
    private long serverTimeDifference = 0;

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

    public void addMarketInfoAsAuditLog(TradeStep step) {
        String msg = "";
        if(TradeUtil.isBuy(step.getSide())) {
            msg += "BUY in " + step.getSymbol() + "; ";
        } else {
            msg += "SELL in " + step.getSymbol() + "; ";
        }
        msg += "Own price: " + String.format("%.8f", step.getPrice()) + "; ";
        if(step.getPriceThreshold() != null) {
            msg += "Threshold: " + String.format("%.8f", step.getPriceThreshold()) + "; ";
        }
        BinanceTicker ticker = marketService.getTicker(step.getSymbol());
        if(ticker != null) {
            msg += "Ticker ask: " + ticker.getAskPrice() + "; ";
            msg += "Ticker bid: " + ticker.getBidPrice() + "; ";
            msg += "Ticker gap: " + String.format("%.2f", 100d * ticker.getPerc()) + "%; ";
            if(ticker.getStats24h() != null) {
                msg += "24h high: " + ticker.getStats24h().getHighPrice() + "; ";
                msg += "24h low: " + ticker.getStats24h().getLowPrice() + "; ";
            }
        }
        try {
            BinanceTrade[] lastTrades = marketService.getLastTrades(step.getSymbol(), 20);
            String sellBuys = "";
            int sells = 0, buys = 0;
            double sellVolume = 0d, buyVolume = 0d;
            for (BinanceTrade trade : lastTrades) {
                if (trade.getIsBuyerMaker()) {
                    sellBuys += "S ";
                    sells++;
                    sellVolume += trade.getQty();
                } else {
                    sellBuys += "B ";
                    buys++;
                    buyVolume += trade.getQty();
                }
            }
            msg += "Last 20 trades: " + buys + " buys (" + String.format("%.8f", buyVolume) + "), " + sells + " sells (" + String.format("%.8f", sellVolume) + "); " + sellBuys;
        } catch (Exception e) {
            msg += "Exception getLastTrades(): " + e.getMessage();
        }

        step.addInfoAuditLog("Market info", msg);
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
        BinanceOrderResult result;
        try {
            result = openLimitOrder(account, offer);
        } catch (BinanceException e) {
            if(e.getCode() == ERROR_CODE_TIME_DIFF) {
                step.setNeedRestart(true);
            }
            throw e;
        } catch (Exception e) {
            step.setNeedRestart(true);
            throw e;
        }

        if(step.getStartDate() == null) {
            step.setStartDate(ZonedDateTime.now());
        }
        step.setFinishDate(null);
        step.setOrderId(result.getOrderId());
        step.setOrderFilled(0d);
        step.setPrice(Double.parseDouble(result.getPrice()));
        step.setOrderAltcoinQty(Double.parseDouble(result.getOrigQty()));
        step.setOrderBasecoinQty(step.getOrderAltcoinQty() * Double.parseDouble(result.getPrice()));
        step.setStatus(TradeStatus.ACTIVE);
        step.setDirty();
        step.setNeedRestart(false);

        String logMsg = "Opened order " + result.getOrderId() + " for plan #" + step.getPlanId() + "-" + step.getStep() + ": ";
        String auditLog;
        if(TradeUtil.isBuy(result.getSide())) {
            auditLog = "BUY " + String.format("%.8f", step.getOrderAltcoinQty()) + " " + step.getOutCurrency() + " for ";
            auditLog += String.format("%.8f", step.getOrderBasecoinQty()) + " " + step.getInCurrency();
        } else {
            auditLog = "SELL " + String.format("%.8f", step.getOrderAltcoinQty()) + " " + step.getInCurrency() + " to ";
            auditLog += String.format("%.8f", step.getOrderBasecoinQty()) + " " + step.getOutCurrency();
        }
        auditLog += " at " + String.format("%.8f", step.getPrice());
        log.info(logMsg + auditLog);
        step.addInfoAuditLog("Order " + result.getOrderId() + " created", auditLog);
        addMarketInfoAsAuditLog(step);
        return result;
    }

    private BinanceOrderResult openLimitOrder(TradeAccount account, TradeOffer offer) {
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

    private BinanceOrderResult getOrderStatus(TradeAccount account, String symbol, long orderId) {
        String params = "symbol=" + symbol;
        params += "&orderId=" + orderId;
        return doSignedGet("/v3/order", params, account, BinanceOrderResult.class);
    }

    public BinanceOrderResult getStepStatus(TradeAccount account, TradeStep step) {
        BinanceOrderResult result = getOrderStatus(account, step.getSymbol(), step.getOrderId());
        double executedAltCoin = Double.parseDouble(result.getExecutedQty()) - step.getOrderFilled();
        if(Math.abs(executedAltCoin) > 0.00000001) {//... stupid double
            step.calcFilling(result);
            if(TradeUtil.isBuy(step.getSide())) {
                step.addInfoAuditLog("Bought " + String.format("%.8f", executedAltCoin) + " " + TradeUtil.getAltCoin(step.getSymbol()), "Bought at " + result.getPrice());
            } else {
                step.addInfoAuditLog("Sold " + String.format("%.8f", executedAltCoin) + " " + TradeUtil.getAltCoin(step.getSymbol()), "Sold at " + result.getPrice());
            }
        }
        return result;
    }

    public BinanceOrderResult cancelOrder(TradeAccount account, String symbol, long orderId) {
        String params = "symbol=" + symbol;
        params += "&orderId=" + orderId;
        log.info("Cancel order " + orderId + " (" + symbol + ")");
        return doSignedDelete("/v3/order", params, account, BinanceOrderResult.class);
    }

    public void cancelStepAndIgnoreStatus(TradeAccount account, TradeStep step) {
        try {
            cancelStep(account, step);
        } catch (Exception e) {
            step.addErrorAuditLog(e.getMessage(), null);
            if(step.getStatus() != TradeStatus.CANCELLED) {
                throw e;
            }
        }
    }

    public BinanceOrderResult cancelStepAndRestartOnError(TradeAccount account, TradeStep step) {
        try {
            return cancelStep(account, step);
        } catch (Exception e) {
            step.addErrorAuditLog(e.getMessage(), null);
            step.setNeedRestart(true);
            throw e;
        }
    }

    public BinanceOrderResult cancelStep(TradeAccount account, TradeStep step) {
        try {
            cancelOrder(account, step.getSymbol(), step.getOrderId());
        } catch(BinanceException e) {
            if(e.getCode() != ERROR_CODE_UNKNOWN_ORDER && e.getCode() != ERROR_CODE_ORDER_NOT_EXIST) {
                throw e;
            }
        }
        step.addInfoAuditLog("Order " + step.getOrderId() + " cancelled");
        step.cancel();
        return getStepStatus(account, step);
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
        BinanceException errorResponse;
        try {
            errorResponse = new ObjectMapper().readValue(e.getResponseBodyAsString(), BinanceException.class);
        } catch (Exception ex) {
            log.error("Not able to parse binance error: " + e.getResponseBodyAsString());
            throw new RuntimeException();
        }
        log.error("binance request ended with " + e.getStatusCode() + ", code " + errorResponse.getCode() + " and msg " + errorResponse.getMsg());
        if(errorResponse.getCode() == ERROR_CODE_TIME_DIFF) {
            calcServerTimeDifference();
        }
        throw errorResponse;
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
