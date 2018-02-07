package io.pacworx.atp.autotrade.controller;

import io.pacworx.atp.autotrade.service.BinanceCircleService;
import io.pacworx.atp.autotrade.service.BinanceService;
import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceAccount;
import io.pacworx.atp.autotrade.domain.binance.BinanceDepth;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/trade/app/binance")
public class BinanceController {

    private final BinanceService binanceService;
    private final BinanceCircleService circleService;
    private final TradeAccountRepository tradeAccountRepository;
    private final TradePlanRepository tradePlanRepository;
    private final TradeCircleRepository tradeCircleRepository;

    @Autowired
    public BinanceController(BinanceService binanceService,
                             BinanceCircleService circleService,
                             TradeAccountRepository tradeAccountRepository,
                             TradePlanRepository tradePlanRepository,
                             TradeCircleRepository tradeCircleRepository) {
        this.binanceService = binanceService;
        this.circleService = circleService;
        this.tradeAccountRepository = tradeAccountRepository;
        this.tradePlanRepository = tradePlanRepository;
        this.tradeCircleRepository = tradeCircleRepository;
    }

    @RequestMapping(value = "/ticker", method = RequestMethod.GET)
    public ResponseEntity<BinanceTicker[]> getTicker() throws Exception {
        return new ResponseEntity<>(binanceService.getAllTicker(), HttpStatus.OK);
    }

    @RequestMapping(value = "/depth/{symbol}", method = RequestMethod.GET)
    public ResponseEntity<BinanceDepth> getDepth(@PathVariable String symbol) throws Exception {
        return new ResponseEntity<>(binanceService.getDepth(symbol), HttpStatus.OK);
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public ResponseEntity<BinanceAccount> getAccount(@ModelAttribute("tradeuser") TradeUser user) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        return new ResponseEntity<>(binanceService.getBinanceAccount(binance), HttpStatus.OK);
    }

    @RequestMapping(value = "/plan/circle", method = RequestMethod.POST)
    public ResponseEntity<TradePlan> createCirclePlan(@ModelAttribute("tradeuser") TradeUser user,
                                                      @Valid @RequestBody CreateCircleRequest request) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = new TradePlan();
        plan.setUserId(user.getId());
        plan.setAccountId(binance.getId());
        plan.setPlanType(TradePlanType.CIRCLE);
        plan.setStatus(TradePlanStatus.ACTIVE);
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
            TradeCircleStep step = new TradeCircleStep();
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
    public ResponseEntity<PlanWithCirclesResponse> cancelPlan(@ModelAttribute("tradeuser") TradeUser user,
                                                              @PathVariable long planId) {
        TradeAccount binance = tradeAccountRepository.findByUserIdAndAndBroker(user.getId(), "binance");
        if(binance == null) {
            throw new BadRequestException("User doesn't have a binance account");
        }
        TradePlan plan = this.tradePlanRepository.findOne(planId);
        if(plan == null || plan.getUserId() != user.getId()) {
            throw new BadRequestException("User is not the owner of requested plan");
        }
        List<TradeCircle> circles = this.circleService.cancelCircles(binance, plan);
        plan.setStatus(TradePlanStatus.CANCELLED);
        this.tradePlanRepository.save(plan);
        return new ResponseEntity<>(new PlanWithCirclesResponse(plan, circles), HttpStatus.OK);
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
    }

    private static final class CreateCircleStep {
        @NotNull
        public String symbol;
        @NotNull
        public String side;
        @NotNull
        public double price;
    }

    private static final class PlanWithCirclesResponse {
        public TradePlan plan;
        public List<TradeCircle> circles;

        public PlanWithCirclesResponse(TradePlan plan, List<TradeCircle> circles) {
            this.plan = plan;
            this.circles = circles;
        }
    }
}
