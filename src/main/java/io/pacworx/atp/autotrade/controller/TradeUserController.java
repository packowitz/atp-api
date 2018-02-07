package io.pacworx.atp.autotrade.controller;

import io.pacworx.atp.autotrade.domain.TradeAccount;
import io.pacworx.atp.autotrade.domain.TradeAccountRepository;
import io.pacworx.atp.autotrade.domain.TradeUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/trade/app/user")
public class TradeUserController {

    private final TradeAccountRepository tradeAccountRepository;

    @Autowired
    public TradeUserController(TradeAccountRepository tradeAccountRepository) {
        this.tradeAccountRepository = tradeAccountRepository;
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<UserWithBinanceResponse> getMe(@ModelAttribute("tradeuser") TradeUser user) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        return new ResponseEntity<>(new UserWithBinanceResponse(user, binance), HttpStatus.OK);
    }

    @RequestMapping(value = "/add/binance", method = RequestMethod.POST)
    public ResponseEntity<UserWithBinanceResponse> addBinance(@ModelAttribute("tradeuser") TradeUser user, @Valid @RequestBody CreateAccountRequest request) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            binance = new TradeAccount();
        }
        binance.setUserId(user.getId());
        binance.setBroker("binance");
        binance.setApiKey(request.apiKey);
        binance.setPrivateKey(request.privateKey);
        tradeAccountRepository.save(binance);
        return new ResponseEntity<>(new UserWithBinanceResponse(user, binance), HttpStatus.OK);
    }

    private static final class UserWithBinanceResponse {
        public TradeUser user;
        public TradeAccount binance;

        UserWithBinanceResponse(TradeUser user, TradeAccount binance) {
            this.user = user;
            this.binance = binance;
        }
    }

    private static final class CreateAccountRequest {
        public String apiKey;
        public String privateKey;
    }
}
