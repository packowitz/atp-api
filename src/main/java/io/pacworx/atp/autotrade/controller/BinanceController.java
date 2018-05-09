package io.pacworx.atp.autotrade.controller;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceAccount;
import io.pacworx.atp.autotrade.domain.binance.BinanceDepth;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.autotrade.domain.binance.BinanceTrade;
import io.pacworx.atp.autotrade.service.BinanceDepthService;
import io.pacworx.atp.autotrade.service.BinanceMarketService;
import io.pacworx.atp.autotrade.service.BinancePlanService;
import io.pacworx.atp.autotrade.service.BinanceOrderService;
import io.pacworx.atp.autotrade.service.strategies.firstMarket.FirstMarketStrategies;
import io.pacworx.atp.autotrade.service.strategies.firstStepPrice.FirstStepPriceStrategies;
import io.pacworx.atp.autotrade.service.strategies.nextMarket.NextMarketStrategies;
import io.pacworx.atp.exception.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trade/app/binance")
public class BinanceController {
    private static final Logger log = LogManager.getLogger();

    private final BinanceOrderService orderService;
    private final BinanceMarketService marketService;
    private final BinancePlanService planService;
    private final BinanceDepthService depthService;
    private final TradeAccountRepository accountRepository;
    private final TradePlanRepository planRepository;
    private final TradeStepRepository stepRepository;
    private final TradeAuditLogRepository auditLogRepository;
    private final TradePlanConfigRepository planConfigRepository;

    @Autowired
    public BinanceController(BinanceOrderService orderService,
                             BinanceMarketService marketService,
                             BinancePlanService planService,
                             BinanceDepthService depthService,
                             TradeAccountRepository accountRepository,
                             TradePlanRepository planRepository,
                             TradeStepRepository stepRepository,
                             TradeAuditLogRepository auditLogRepository,
                             TradePlanConfigRepository planConfigRepository) {
        this.orderService = orderService;
        this.marketService = marketService;
        this.planService = planService;
        this.depthService = depthService;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.auditLogRepository = auditLogRepository;
        this.planConfigRepository = planConfigRepository;
    }

    @RequestMapping(value = "/ticker", method = RequestMethod.GET)
    public ResponseEntity<BinanceTicker[]> getTicker() throws Exception {
        return new ResponseEntity<>(marketService.getAllTicker(), HttpStatus.OK);
    }

    @RequestMapping(value = "/depth/{symbol}", method = RequestMethod.GET)
    public ResponseEntity<BinanceDepth> getDepth(@PathVariable String symbol) throws Exception {
        return new ResponseEntity<>(depthService.getDepth(symbol), HttpStatus.OK);
    }

    @RequestMapping(value = "/trades/{symbol}", method = RequestMethod.GET)
    public ResponseEntity<BinanceTrade[]> getTrades(@PathVariable String symbol) throws Exception {
        return new ResponseEntity<>(marketService.getLastTrades(symbol, 500), HttpStatus.OK);
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public ResponseEntity<BinanceAccount> getAccount(@ModelAttribute("tradeuser") TradeUser user) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        return new ResponseEntity<>(orderService.getBinanceAccount(binance), HttpStatus.OK);
    }

    @RequestMapping(value = "/plan", method = RequestMethod.POST)
    public ResponseEntity<TradePlan> createPlan(@ModelAttribute("tradeuser") TradeUser user,
                                                @RequestBody TradePlanConfig config) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = new TradePlan(binance, TradePlanType.ONEMARKET);
        plan.setDescription("Trade " + config.getStartAmount() + " " + config.getStartCurrency());
        this.planRepository.save(plan);

        config.setPlanId(plan.getId());
        this.planConfigRepository.save(config);
        plan.setConfig(config);
        this.planService.startPlan(binance, plan);

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/plans", method = RequestMethod.GET)
    public ResponseEntity<List<TradePlan>> getPlans(@ModelAttribute("tradeuser") TradeUser user) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        List<TradePlan> plans = this.planRepository.findAllByAccountIdOrderByIdDesc(binance.getId());
        for(TradePlan plan: plans) {
            plan.setConfig(this.planConfigRepository.findOne(plan.getId()));
        }
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}", method = RequestMethod.GET)
    public ResponseEntity<TradePlan> getPlan(@ModelAttribute("tradeuser") TradeUser user,
                                             @PathVariable long planId) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.planRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        plan.setConfig(this.planConfigRepository.findOne(plan.getId()));
        List<TradeStep> steps = stepRepository.findAllByPlanIdOrderByIdDesc(plan.getId());
        plan.setSteps(steps);
        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/step/{stepId}/logs", method = RequestMethod.GET)
    public ResponseEntity<List<TradeAuditLog>> getAuditLogs(@PathVariable long stepId) {
        List<TradeAuditLog> logs = auditLogRepository.findFirst100ByStepIdOrderByTimestampDesc(stepId);
        return new ResponseEntity<>(logs, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/step/{stepId}/removeThreshold", method = RequestMethod.POST)
    public ResponseEntity<TradeStep> removeThreshold(@ModelAttribute("tradeuser") TradeUser user,
                                                     @PathVariable long planId,
                                                     @PathVariable long stepId) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.planRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        TradeStep step = stepRepository.findOne(stepId);
        if(step == null || step.getPlanId() != planId) {
            throw new BadRequestException("Unknown step");
        }
        step.setPriceThreshold(null);
        stepRepository.save(step);
        return new ResponseEntity<>(step, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/autorepeat/{autorepeat}", method = RequestMethod.PUT)
    public ResponseEntity<TradePlanConfig> updateAutorepeat(@ModelAttribute("tradeuser") TradeUser user,
                                                            @PathVariable long planId,
                                                            @PathVariable boolean autorepeat) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.planRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        TradePlanConfig config = planConfigRepository.findOne(planId);
        config.setAutoRestart(autorepeat);
        planConfigRepository.save(config);

        return new ResponseEntity<>(config, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/cancel", method = RequestMethod.GET)
    public ResponseEntity<TradePlan> cancelPlan(@ModelAttribute("tradeuser") TradeUser user,
                                                              @PathVariable long planId) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.planRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        log.info("User " + user.getId() + " manually cancelled plan " + planId);
        this.planService.cancelPlan(binance, plan);
        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}", method = RequestMethod.DELETE)
    public ResponseEntity<TradePlan> deletePlan(@ModelAttribute("tradeuser") TradeUser user,
                                                @PathVariable long planId) {
        TradeAccount binance = accountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.planRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan " + planId);
        }
        if(plan.getStatus() == TradePlanStatus.ACTIVE) {
            throw new BadRequestException("Cannot delete plan " + planId + " because it is active.");
        }
        log.info("User " + user.getId() + " manually deleted plan " + planId);
        this.planService.deletePlan(plan);
        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/config/firstmarket", method = RequestMethod.GET)
    public ResponseEntity<FirstMarketStrategies[]> getFirstMarketStrategies() {
        return new ResponseEntity<>(FirstMarketStrategies.values(), HttpStatus.OK);
    }

    @RequestMapping(value = "/config/firststepprice", method = RequestMethod.GET)
    public ResponseEntity<FirstStepPriceStrategies[]> getFirstStepPriceStrategies() {
        return new ResponseEntity<>(FirstStepPriceStrategies.values(), HttpStatus.OK);
    }

    @RequestMapping(value = "/config/nextmarket", method = RequestMethod.GET)
    public ResponseEntity<NextMarketStrategies[]> getNextMarketStrategies() {
        return new ResponseEntity<>(NextMarketStrategies.values(), HttpStatus.OK);
    }
}
