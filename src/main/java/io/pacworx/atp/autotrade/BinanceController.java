package io.pacworx.atp.autotrade;

import io.pacworx.atp.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/trade/app/binance")
public class BinanceController {

    private static final String SERVER = "https://api.binance.com/api";

    private final TradeAccountRepository tradeAccountRepository;

    @Autowired
    public BinanceController(TradeAccountRepository tradeAccountRepository) {
        this.tradeAccountRepository = tradeAccountRepository;
    }

    @RequestMapping(value = "/ticker", method = RequestMethod.GET)
    public ResponseEntity<Ticker[]> getTicker() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        Ticker[] tickers = restTemplate.getForObject(SERVER + "/v1/ticker/allBookTickers", Ticker[].class);

        for(Ticker ticker : tickers) {
            double ask = Double.parseDouble(ticker.getAskPrice());
            double bid = Double.parseDouble(ticker.getBidPrice());
            double perc = (ask / bid) - 1;
            ticker.setPerc(perc);
        }

        return new ResponseEntity<>(tickers, HttpStatus.OK);
    }

    @RequestMapping(value = "/depth/{symbol}", method = RequestMethod.GET)
    public ResponseEntity<Depth> getDepth(@PathVariable String symbol) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        Depth depth = restTemplate.getForObject(SERVER + "/v1/depth?symbol=" + symbol, Depth.class);
        return new ResponseEntity<>(depth, HttpStatus.OK);
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public ResponseEntity<BinanceAccount> getAccount(@ModelAttribute("tradeuser") TradeUser user) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User already has a binance account");
        }
        BinanceAccount binanceAccount = doSignedGet("/v3/account", null, binance, BinanceAccount.class);
        return new ResponseEntity<>(binanceAccount, HttpStatus.OK);
    }

    private <T>T doSignedGet(String path, String params, TradeAccount account, Class<T> returnClass) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER + path;
        if(params == null || params.length() == 0) {
            params = "";
        } else {
            params += "&";
        }
        params += "recvWindow=5000&timestamp=" + System.currentTimeMillis();
        url += "?" + params + "&signature=" + getSignature(params, account.getPrivateKeyUnencrypted());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", account.getApiKeyUnencrypted());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET,  entity, returnClass);
        return response.getBody();
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
}
