package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class BinancePathService {
    private static final Logger log = LogManager.getLogger();

    private final BinanceService binanceService;
    private final TradePathRepository pathRepository;
    private final TradeOrderObserverRepository orderObserverRepository;
    private final TradeAccountRepository accountRepository;
    private final TradePlanRepository planRepository;

    @Autowired
    public BinancePathService(BinanceService binanceService,
                              TradePathRepository pathRepository,
                              TradeOrderObserverRepository orderObserverRepository,
                              TradeAccountRepository accountRepository,
                              TradePlanRepository planRepository) {
        this.binanceService = binanceService;
        this.pathRepository = pathRepository;
        this.orderObserverRepository = orderObserverRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
    }

    public void startPath(TradeAccount account, TradePath path) {
        RouteCalculator.Route route = findBestRoute(path.getMaxSteps(), path.getStartCurrency(), path.getDestCurrency());
        RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

        TradeStep firstStep = createTradeStep(routeFirstStep, 1, path.getStartCurrency(), path.getStartAmount());
        path.addStep(firstStep);

        BinanceOrderResult result = openStepOrder(account, firstStep);

        pathRepository.save(path);

        TradeOrderObserver observer = new TradeOrderObserver();
        observer.setOrderId(result.getOrderId());
        observer.setSymbol(firstStep.getSymbol());
        observer.setBroker("binance");
        observer.setUserId(account.getUserId());
        observer.setAccountId(account.getId());
        observer.setPlanId(path.getPlanId());
        observer.setPlanType(TradePlanType.PATH);
        observer.setSubplanId(path.getId());
        observer.setTreshold(99);
        observer.setCancelOnTreshold(true);
        observer.setCheckDate(ZonedDateTime.now());
        orderObserverRepository.save(observer);
    }

    public void cancelPaths(TradeAccount account, TradePlan plan) {
        List<TradeOrderObserver> orders = orderObserverRepository.getAllByPlanId(plan.getId());
        for(TradeOrderObserver order: orders) {
            binanceService.cancelOrder(account, order.getSymbol(), order.getOrderId());
            orderObserverRepository.delete(order.getId());
        }
        List<TradePath> paths = pathRepository.findAllByPlanIdAndStatus(plan.getId(), TradePlanStatus.ACTIVE);
        for(TradePath path: paths) {
            path.setStatus(TradePlanStatus.CANCELLED);
            TradeStep latestStep = path.getLatestStep();
            if(latestStep != null) {
                latestStep.setStatus(TradeStatus.CANCELLED);
            }
            pathRepository.save(path);
        }
    }

    @Scheduled(fixedDelay = 20000)
    public void checkOrders() {
        List<TradeOrderObserver> ordersToCheck = orderObserverRepository.getAllByPlanType(TradePlanType.PATH);
        for(TradeOrderObserver orderToCheck: ordersToCheck) {
            try {
                TradeAccount account = accountRepository.findOne(orderToCheck.getAccountId());
                BinanceOrderResult orderResult = binanceService.getOrderStatus(account, orderToCheck.getSymbol(), orderToCheck.getOrderId());
                if ("FILLED".equals(orderResult.getStatus())) {
                    log.info("Order " + orderResult.getOrderId() + " is filled. Setting up next path step.");
                    startNextStep(account, orderToCheck, orderResult);
                } if("CANCELED".equals(orderResult.getStatus())) {
                    log.info("Order " + orderResult.getOrderId() + " was cancelled. Deleting observer.");
                    orderObserverRepository.delete(orderToCheck.getId());
                    planRepository.updateStatus(orderToCheck.getPlanId(), TradePlanStatus.CANCELLED.name());
                    TradePath path = pathRepository.findOne(orderToCheck.getSubplanId());
                    path.setStatus(TradePlanStatus.CANCELLED);
                    TradeStep step = path.getLatestStep();
                    if(step != null) {
                        step.setStatus(TradeStatus.CANCELLED);
                    }
                    pathRepository.save(path);
                } if("PARTIALLY_FILLED".equals(orderResult.getStatus())) {
                    double percExecuted = 100d * Double.parseDouble(orderResult.getExecutedQty()) / Double.parseDouble(orderResult.getOrigQty());
                    if(percExecuted >= orderToCheck.getTreshold()) {
                        log.info("Order " + orderResult.getOrderId() + " is filled over " + orderToCheck.getTreshold() + "%. Setting up next path step.");
                        if(orderToCheck.isCancelOnTreshold()) {
                            binanceService.cancelOrder(account, orderToCheck.getSymbol(), orderToCheck.getOrderId());
                        }
                        startNextStep(account, orderToCheck, orderResult);
                    } else {
                        log.info("Order " + orderResult.getOrderId() + " is partially filled under " + orderToCheck.getTreshold() + "%.");
                    }
                } if("NEW".equals(orderResult.getStatus())) {
                    long duration = Duration.between(orderToCheck.getCheckDate(), ZonedDateTime.now()).getSeconds();
                    if(duration >= 300) {
                        log.info("Order " + orderResult.getOrderId() + " from path " + orderToCheck.getSubplanId() + " is since " + duration + "s in status NEW. Will cancel step and search for better one.");
                        binanceService.cancelOrder(account, orderToCheck.getSymbol(), orderToCheck.getOrderId());
                        restartLatestStep(account, orderToCheck);
                    } else {
                        log.info("Order " + orderResult.getOrderId() + " from path " + orderToCheck.getSubplanId() + " is since " + duration + "s in status NEW");
                    }
                } else {
                    log.info("Order " + orderResult.getOrderId() + " from path " + orderToCheck.getSubplanId() + " is in status: " + orderResult.getStatus());
                }
            } catch (Exception e) {
                log.info("Order " + orderToCheck.getOrderId() + " from path " + orderToCheck.getSubplanId() + " failed to check status");
            }
        }
    }

    private void restartLatestStep(TradeAccount account, TradeOrderObserver orderToCheck) {
        TradePath path = pathRepository.findOne(orderToCheck.getSubplanId());
        TradeStep currentStep = path.getLatestStep();
        currentStep.setStatus(TradeStatus.CANCELLED);

        RouteCalculator.Route route = findBestRoute(path.getMaxSteps() - path.getStepsCompleted(), currentStep.getInCurrency(), path.getDestCurrency());
        RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

        TradeStep step = createTradeStep(routeFirstStep, path.getStepsCompleted() + 1, currentStep.getInCurrency(), currentStep.getInAmount());
        path.addStep(step);

        BinanceOrderResult result = openStepOrder(account, step);

        orderToCheck.setOrderId(result.getOrderId());
        orderToCheck.setSymbol(step.getSymbol());
        orderToCheck.setCheckDate(ZonedDateTime.now());
        orderObserverRepository.save(orderToCheck);
        pathRepository.save(path);
    }

    private void startNextStep(TradeAccount account, TradeOrderObserver orderToCheck, BinanceOrderResult orderResult) {
        TradePath path = pathRepository.findOne(orderToCheck.getSubplanId());
        TradeStep currentStep = path.getLatestStep();
        currentStep.setStatus(TradeStatus.DONE);
        currentStep.setFinishDate(ZonedDateTime.now());
        currentStep.setOutCurrency(TradeUtil.otherCur(orderResult.getSymbol(), currentStep.getInCurrency()));
        double inAmount, outAmount;
        if(TradeUtil.isBuy(orderResult.getSide())) {
            inAmount = Double.parseDouble(orderResult.getExecutedQty()) * Double.parseDouble(orderResult.getPrice());
            outAmount = Double.parseDouble(orderResult.getExecutedQty());
        } else {
            inAmount = Double.parseDouble(orderResult.getExecutedQty());
            outAmount = Double.parseDouble(orderResult.getExecutedQty()) * Double.parseDouble(orderResult.getPrice());
        }
        currentStep.setInAmount(inAmount);
        currentStep.setOutAmount(outAmount);

        if(currentStep.getOutCurrency().equals(path.getDestCurrency())) {
            //Path finished
            orderObserverRepository.delete(orderToCheck.getId());
            path.setStatus(TradePlanStatus.FINISHED);
            path.setFinishDate(ZonedDateTime.now());
            path.setDestAmount(currentStep.getOutAmount());

            if(path.isAutoRestart()) {
                startPath(account, new TradePath(path));
            } else {
                planRepository.updateStatus(path.getPlanId(), TradePlanStatus.FINISHED.name());
            }
        } else {
            RouteCalculator.Route route = findBestRoute(path.getMaxSteps() - path.getStepsCompleted(), currentStep.getOutCurrency(), path.getDestCurrency());
            RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

            TradeStep step = createTradeStep(routeFirstStep, path.getStepsCompleted() + 1, currentStep.getOutCurrency(), currentStep.getOutAmount());
            path.addStep(step);

            BinanceOrderResult result = openStepOrder(account, step);

            orderToCheck.setOrderId(result.getOrderId());
            orderToCheck.setSymbol(step.getSymbol());
            orderToCheck.setCheckDate(ZonedDateTime.now());
            orderObserverRepository.save(orderToCheck);
        }
        pathRepository.save(path);
    }

    private TradeStep createTradeStep(RouteCalculator.RouteStep routeStep, int stepNumber, String inCurrency, double inAmount) {
        TradeStep step = new TradeStep();
        step.setStep(stepNumber);
        step.setStatus(TradeStatus.IDLE);
        step.setSymbol(routeStep.ticker.getSymbol());
        step.setSide(routeStep.isBuy ? "BUY" : "SELL");
        step.setPrice(routeStep.tradePoint);
        step.setInCurrency(inCurrency);
        step.setInAmount(inAmount);
        return step;
    }

    private RouteCalculator.Route findBestRoute(int maxSteps, String startCur, String destCur) {
        List<BinanceTicker> tickers = Arrays.asList(binanceService.getAllTicker());
        RouteCalculator calculator = new RouteCalculator(maxSteps, startCur, destCur, tickers);
        return calculator.searchBestRoute();
    }

    private BinanceOrderResult openStepOrder(TradeAccount account, TradeStep step) {
        double amount;
        if(TradeUtil.isBuy(step.getSide())) {
            amount = step.getInAmount() / step.getPrice();
        } else {
            amount = step.getInAmount();
        }

        TradeOffer offer = new TradeOffer(step.getSymbol(), step.getSide().toUpperCase(), step.getPrice(), amount);
        BinanceOrderResult result = binanceService.openLimitOrder(account, offer);

        step.setStartDate(ZonedDateTime.now());
        step.setOrderId(result.getOrderId());
        step.setStatus(TradeStatus.ACTIVE);

        return result;
    }

}
