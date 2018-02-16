package io.pacworx.atp.autotrade.controller;

import io.pacworx.atp.autotrade.domain.binance.BinanceTrade;
import io.pacworx.atp.autotrade.service.BinanceCircleService;
import io.pacworx.atp.autotrade.service.BinanceDepthService;
import io.pacworx.atp.autotrade.service.BinancePathService;
import io.pacworx.atp.autotrade.service.BinanceService;
import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceAccount;
import io.pacworx.atp.autotrade.domain.binance.BinanceDepth;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
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
    private final BinanceCircleService circleService;
    private final BinancePathService pathService;
    private final BinanceDepthService depthService;
    private final TradeAccountRepository tradeAccountRepository;
    private final TradePlanRepository tradePlanRepository;
    private final TradeCircleRepository tradeCircleRepository;
    private final TradePathRepository tradePathRepository;

    @Autowired
    public BinanceController(BinanceService binanceService,
                             BinanceCircleService circleService,
                             BinancePathService pathService,
                             BinanceDepthService depthService,
                             TradeAccountRepository tradeAccountRepository,
                             TradePlanRepository tradePlanRepository,
                             TradeCircleRepository tradeCircleRepository,
                             TradePathRepository tradePathRepository) {
        this.binanceService = binanceService;
        this.circleService = circleService;
        this.pathService = pathService;
        this.depthService = depthService;
        this.tradeAccountRepository = tradeAccountRepository;
        this.tradePlanRepository = tradePlanRepository;
        this.tradeCircleRepository = tradeCircleRepository;
        this.tradePathRepository = tradePathRepository;
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
        return new ResponseEntity<>(binanceService.getLastTrades(symbol), HttpStatus.OK);
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

    @RequestMapping(value = "/plan/circle", method = RequestMethod.POST)
    public ResponseEntity<TradePlan> createCirclePlan(@ModelAttribute("tradeuser") TradeUser user,
                                                      @Valid @RequestBody CreateCircleRequest request) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = new TradePlan(binance, TradePlanType.CIRCLE);
        plan.setDescription(request.createDescription());
        this.tradePlanRepository.save(plan);

        TradeCircle circle = new TradeCircle();
        circle.setPlanId(plan.getId());
        circle.setStatus(TradePlanStatus.ACTIVE);
        circle.setStartCurrency(request.startCurrency);
        circle.setStartAmount(request.startAmount);
        circle.setRisk(request.risk);
        circle.setTreshold(request.treshold);
        circle.setCancelOnTreshold(request.cancelOnTreshold);
        int stepCount = 0;
        for(CreateCircleStep createStep: request.steps) {
            TradeStep step = new TradeStep();
            step.setStep(++stepCount);
            step.setStatus(TradeStatus.IDLE);
            step.setSymbol(createStep.symbol);
            step.setSide(createStep.side);
            step.setPrice(createStep.price);
            circle.addStep(step);
        }
        this.tradeCircleRepository.save(circle);
        this.circleService.startCircle(binance, circle);
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
        return new ResponseEntity<>(paths, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/autorepeat/{autorepeat}", method = RequestMethod.PUT)
    public ResponseEntity<List<TradePath>> changePathAutorepeat(@ModelAttribute("tradeuser") TradeUser user,
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
        List<TradePath> paths = this.tradePathRepository.findAllByPlanIdOrderByStartDateDesc(planId);
        if(!paths.isEmpty()) {
            TradePath latestPath = paths.get(0);
            latestPath.setAutoRestart(autorepeat);
            this.tradePathRepository.save(latestPath);
        }
        return new ResponseEntity<>(paths, HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/{planId}/circles", method = RequestMethod.GET)
    public ResponseEntity<List<TradeCircle>> getCircle(@ModelAttribute("tradeuser") TradeUser user,
                                                       @PathVariable long planId) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.tradePlanRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        List<TradeCircle> circles = this.tradeCircleRepository.findAllByPlanId(planId);
        return new ResponseEntity<>(circles, HttpStatus.OK);
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
        } else if(plan.getType() == TradePlanType.CIRCLE) {
            this.circleService.cancelCircles(binance, plan);
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
        } else if(plan.getType() == TradePlanType.CIRCLE) {
            this.circleService.deleteCircles(plan);
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

    private static final class CreateCircleRequest {
        @NotNull
        public String startCurrency;
        @NotNull
        public double startAmount;
        @NotNull
        public TradeCircleRisk risk;
        @NotNull
        public Integer treshold;
        @NotNull
        public Boolean cancelOnTreshold;
        @NotNull
        public List<CreateCircleStep> steps;

        public String createDescription() {
            String desc = "" + startAmount + " " + startCurrency;
            String prevCur = startCurrency;
            for(CreateCircleStep step: steps) {
                prevCur = step.symbol.replaceFirst(prevCur, "");
                desc += "->" + prevCur;
            }
            return desc;
        }
    }

    private static final class CreateCircleStep {
        @NotNull
        public String symbol;
        @NotNull
        public String side;
        @NotNull
        public double price;
    }
}
