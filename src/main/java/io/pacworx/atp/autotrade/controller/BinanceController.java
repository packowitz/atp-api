package io.pacworx.atp.autotrade.controller;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceAccount;
import io.pacworx.atp.autotrade.domain.binance.BinanceDepth;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.autotrade.domain.binance.BinanceTrade;
import io.pacworx.atp.autotrade.service.BinanceDepthService;
import io.pacworx.atp.autotrade.service.BinanceOneMarketService;
import io.pacworx.atp.autotrade.service.BinancePathService;
import io.pacworx.atp.autotrade.service.BinanceService;
import io.pacworx.atp.exception.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/trade/app/binance")
public class BinanceController {
    private static final Logger log = LogManager.getLogger();

    private final BinanceService binanceService;
    private final BinancePathService pathService;
    private final BinanceOneMarketService oneMarketService;
    private final BinanceDepthService depthService;
    private final TradeAccountRepository tradeAccountRepository;
    private final TradePlanRepository tradePlanRepository;
    private final TradePathRepository tradePathRepository;
    private final TradeOneMarketRepository tradeOneMarketRepository;
    private final TradeStepRepository tradeStepRepository;
    private final TradeAuditLogRepository auditLogRepository;

    @Autowired
    public BinanceController(BinanceService binanceService,
                             BinancePathService pathService,
                             BinanceOneMarketService oneMarketService,
                             BinanceDepthService depthService,
                             TradeAccountRepository tradeAccountRepository,
                             TradePlanRepository tradePlanRepository,
                             TradePathRepository tradePathRepository,
                             TradeOneMarketRepository tradeOneMarketRepository,
                             TradeStepRepository tradeStepRepository,
                             TradeAuditLogRepository auditLogRepository) {
        this.binanceService = binanceService;
        this.pathService = pathService;
        this.oneMarketService = oneMarketService;
        this.depthService = depthService;
        this.tradeAccountRepository = tradeAccountRepository;
        this.tradePlanRepository = tradePlanRepository;
        this.tradePathRepository = tradePathRepository;
        this.tradeOneMarketRepository = tradeOneMarketRepository;
        this.tradeStepRepository = tradeStepRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @RequestMapping(value = "/ticker", method = RequestMethod.GET)
    public ResponseEntity<BinanceTicker[]> getTicker() throws Exception {
        return new ResponseEntity<>(binanceService.getAllTicker(), HttpStatus.OK);
    }

    @RequestMapping(value = "/depth/{symbol}", method = RequestMethod.GET)
    public ResponseEntity<BinanceDepth> getDepth(@PathVariable String symbol) throws Exception {
        return new ResponseEntity<>(depthService.getDepth(symbol), HttpStatus.OK);
    }

    @RequestMapping(value = "/trades/{symbol}", method = RequestMethod.GET)
    public ResponseEntity<BinanceTrade[]> getTrades(@PathVariable String symbol) throws Exception {
        return new ResponseEntity<>(binanceService.getLastTrades(symbol, 500), HttpStatus.OK);
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public ResponseEntity<BinanceAccount> getAccount(@ModelAttribute("tradeuser") TradeUser user) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        return new ResponseEntity<>(binanceService.getBinanceAccount(binance), HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/path", method = RequestMethod.POST)
    public ResponseEntity<TradePlan> createPathPlan(@ModelAttribute("tradeuser") TradeUser user,
                                                    @Valid @RequestBody CreatePathRequest request) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if (binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = new TradePlan(binance, TradePlanType.PATH);
        plan.setDescription(request.createDescription());

        this.tradePlanRepository.save(plan);
        TradePath path = new TradePath(plan, request);
        this.pathService.startPath(binance, path);

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/onemarket", method = RequestMethod.POST)
    public ResponseEntity<TradePlan> createOneMarket(@ModelAttribute("tradeuser") TradeUser user,
                                                     @Valid @RequestBody CreateOneMarketRequest request) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = new TradePlan(binance, TradePlanType.ONEMARKET);
        plan.setDescription(request.createDescription());
        this.tradePlanRepository.save(plan);

        TradeOneMarket oneMarket = new TradeOneMarket(plan, request);
        this.oneMarketService.startPlan(binance, oneMarket);

        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/plans", method = RequestMethod.GET)
    public ResponseEntity<List<TradePlan>> getPlans(@ModelAttribute("tradeuser") TradeUser user) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        List<TradePlan> plans = this.tradePlanRepository.findAllByAccountIdOrderByIdDesc(binance.getId());
        return new ResponseEntity<>(plans, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/onemarket", method = RequestMethod.GET)
    public ResponseEntity<TradeOneMarket> getOneMarket(@ModelAttribute("tradeuser") TradeUser user,
                                                       @PathVariable long planId) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.tradePlanRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        TradeOneMarket oneMarket = tradeOneMarketRepository.findByPlanId(planId);
        List<TradeStep> steps = tradeStepRepository.findAllByPlanIdAndSubplanIdOrderByIdDesc(oneMarket.getPlanId(), oneMarket.getId());
        oneMarket.setSteps(steps);
        return new ResponseEntity<>(oneMarket, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/paths", method = RequestMethod.GET)
    public ResponseEntity<List<TradePath>> getPaths(@ModelAttribute("tradeuser") TradeUser user,
                                                    @PathVariable long planId) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.tradePlanRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        List<TradePath> paths = this.tradePathRepository.findAllByPlanIdOrderByStartDateDesc(planId);
        for(TradePath path: paths) {
            List<TradeStep> steps = tradeStepRepository.findAllByPlanIdAndSubplanIdOrderByIdDesc(path.getPlanId(), path.getId());
            path.setSteps(steps);
        }
        return new ResponseEntity<>(paths, HttpStatus.OK);
    }

    @RequestMapping(value = "/step/{stepId}/logs", method = RequestMethod.GET)
    public ResponseEntity<List<TradeAuditLog>> getAuditLogs(@PathVariable long stepId) {
        List<TradeAuditLog> logs = auditLogRepository.findAllByStepIdOrderByTimestampDesc(stepId);
        return new ResponseEntity<>(logs, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/autorepeat/{autorepeat}", method = RequestMethod.PUT)
    public ResponseEntity<Object> changePathAutorepeat(@ModelAttribute("tradeuser") TradeUser user,
                                                                @PathVariable long planId,
                                                                @PathVariable boolean autorepeat) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.tradePlanRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        if(plan.getType() == TradePlanType.PATH) {
            List<TradePath> paths = this.tradePathRepository.findAllByPlanIdOrderByStartDateDesc(planId);
            if(!paths.isEmpty()) {
                TradePath latestPath = paths.get(0);
                latestPath.setAutoRestart(autorepeat);
                this.tradePathRepository.save(latestPath);
            }
            for(TradePath path: paths) {
                List<TradeStep> steps = tradeStepRepository.findAllByPlanIdAndSubplanIdOrderByIdDesc(path.getPlanId(), path.getId());
                path.setSteps(steps);
            }
            return new ResponseEntity<>(paths, HttpStatus.OK);
        }
        if(plan.getType() == TradePlanType.ONEMARKET) {
            TradeOneMarket oneMarket = this.tradeOneMarketRepository.findByPlanId(planId);
            oneMarket.setAutoRestart(autorepeat);
            this.tradeOneMarketRepository.save(oneMarket);
            List<TradeStep> steps = tradeStepRepository.findAllByPlanIdAndSubplanIdOrderByIdDesc(oneMarket.getPlanId(), oneMarket.getId());
            oneMarket.setSteps(steps);
            return new ResponseEntity<>(oneMarket, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/plan/{planId}/cancel", method = RequestMethod.GET)
    public ResponseEntity<TradePlan> cancelPlan(@ModelAttribute("tradeuser") TradeUser user,
                                                              @PathVariable long planId) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.tradePlanRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        log.info("User " + user.getId() + " manually cancelled plan " + planId);
        if(plan.getType() == TradePlanType.PATH) {
            this.pathService.cancelPaths(binance, plan);
        } else if(plan.getType() == TradePlanType.ONEMARKET) {
            this.oneMarketService.cancelPlan(binance, plan);
        }
        plan.setStatus(TradePlanStatus.CANCELLED);
        this.tradePlanRepository.save(plan);
        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}", method = RequestMethod.DELETE)
    public ResponseEntity<TradePlan> deletePlan(@ModelAttribute("tradeuser") TradeUser user,
                                                @PathVariable long planId) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.tradePlanRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan " + planId);
        }
        if(plan.getStatus() == TradePlanStatus.ACTIVE) {
            throw new BadRequestException("Cannot delete plan " + planId + " because it is active.");
        }
        log.info("User " + user.getId() + " manually deleted plan " + planId);
        if(plan.getType() == TradePlanType.PATH) {
            this.pathService.deletePaths(plan);
        } else if(plan.getType() == TradePlanType.ONEMARKET) {
            this.oneMarketService.deletePlan(plan);
        }
        tradePlanRepository.delete(plan);
        return new ResponseEntity<>(plan, HttpStatus.OK);
    }

    public static final class CreatePathRequest {
        @NotNull
        public String startCurrency;
        @NotNull
        public double startAmount;
        @NotNull
        public String destCurrency;
        @NotNull
        @Min(2)
        @Max(6)
        public int maxSteps;
        public boolean autoRestart;

        public String createDescription() {
            return startAmount + " " + startCurrency + " in " + maxSteps + " steps to " + destCurrency;
        }
    }

    public static final class CreateOneMarketRequest {
        @NotNull
        public String symbol;
        @NotNull
        public double minProfit;
        @NotNull
        public String startCurrency;
        @NotNull
        public double startAmount;
        public boolean autoRestart;

        public String createDescription() {
            return "Trade " + startAmount + " " + startCurrency + " in " + symbol;
        }
    }
}
