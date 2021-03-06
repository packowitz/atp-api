package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.exception.BinanceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class BinancePathService {
    private static final Logger log = LogManager.getLogger();
    private static final String SCHEDULE_NAME = "PATHCHECK";
    private static final String SCHEDULE_PAUSE_NAME = "PATHPAUSEDCHECK";

    private final BinanceOrderService orderService;
    private final BinanceDepthService depthService;
    private final TradePathRepository pathRepository;
    private final TradeAccountRepository accountRepository;
    private final TradePlanRepository planRepository;
    private final TradeStepRepository stepRepository;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final TradeAuditLogRepository auditLogRepository;
    private final TradeScheduleLockRepository scheduleLockRepository;
    private final BinanceMarketService marketService;

    @Autowired
    public BinancePathService(BinanceOrderService orderService,
                              BinanceDepthService depthService,
                              TradePathRepository pathRepository,
                              TradeAccountRepository accountRepository,
                              TradePlanRepository planRepository,
                              TradeStepRepository stepRepository,
                              BinanceExchangeInfoService exchangeInfoService,
                              TradeAuditLogRepository auditLogRepository,
                              TradeScheduleLockRepository scheduleLockRepository,
                              BinanceMarketService marketService) {
        this.orderService = orderService;
        this.depthService = depthService;
        this.pathRepository = pathRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.exchangeInfoService = exchangeInfoService;
        this.auditLogRepository = auditLogRepository;
        this.scheduleLockRepository = scheduleLockRepository;
        this.marketService = marketService;
    }

    /* SCHEDULER DISABLED!!! THIS IS DEPRECATED CODE */

    public void startPath(TradeAccount account, TradePath path) {
        RouteCalculator.Route route = findBestRoute(path, path.getStartCurrency(), path.getStartAmount());
        if(route != null) {
            path.setStatus(TradePlanStatus.ACTIVE);
            RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

            TradeStep firstStep = createTradeStep(routeFirstStep, 1, path.getStartCurrency(), path.getStartAmount());
            firstStep.setPlanId(path.getPlanId());
            path.addStep(firstStep);

            orderService.openStepOrder(account, firstStep);
            saveSubplan(path);
        } else {
            if(path.getStatus() != TradePlanStatus.PAUSED) {
                log.info("Cannot (re)start path plan #" + path.getPlanId() + " because there is no profitable route. PAUSE it for now");
                path.setStatus(TradePlanStatus.PAUSED);
                saveSubplan(path);
            }
        }
    }

    public void cancelPaths(TradeAccount account, TradePlan plan) {
        List<TradePath> paths = pathRepository.findAllByPlanIdAndStatus(plan.getId(), TradePlanStatus.ACTIVE);
        for(TradePath path: paths) {
            addStepsToMarket(path);
            path.cancel();
            TradeStep latestStep = path.getLatestStep();
            if(latestStep != null) {
                if(latestStep.getStatus() == TradeStatus.ACTIVE) {
                    orderService.cancelOrder(account, latestStep.getSymbol(), latestStep.getOrderId());
                }
                latestStep.cancel();
            }
            saveSubplan(path);
        }
        paths = pathRepository.findAllByPlanIdAndStatus(plan.getId(), TradePlanStatus.PAUSED);
        for(TradePath path: paths) {
            path.cancel();
            saveSubplan(path);
        }
    }

    public void deletePaths(TradePlan plan) {
        pathRepository.deleteAllByPlanId(plan.getId());
        stepRepository.deleteAllByPlanId(plan.getId());
        auditLogRepository.deleteAllByPlanId(plan.getId());
    }

    public void checkPausedPlans() {
        if(!scheduleLockRepository.lock(SCHEDULE_PAUSE_NAME)) {
            return;
        }
        try {
            List<TradePath> pausedPaths = pathRepository.findAllByStatus(TradePlanStatus.PAUSED);
            for (TradePath pausedPath : pausedPaths) {
                addStepsToMarket(pausedPath);
                TradeStep latestStep = pausedPath.getLatestStep();
                TradeAccount account = accountRepository.findOne(pausedPath.getAccountId());
                if (latestStep != null) {
                    startNextStep(account, pausedPath, latestStep);
                } else {
                    startPath(account, pausedPath);
                }
                if (pausedPath.getStatus() == TradePlanStatus.ACTIVE) {
                    log.info("Paused path plan #" + pausedPath.getPlanId() + " reactivated.");
                    saveSubplan(pausedPath);
                }
            }
        } finally {
            scheduleLockRepository.unlock(SCHEDULE_PAUSE_NAME);
        }
    }

    public void checkOrders() {
        if(!scheduleLockRepository.lock(SCHEDULE_NAME)) {
            return;
        }
        try {
            List<TradePath> activePaths = pathRepository.findAllByStatus(TradePlanStatus.ACTIVE);
            for (TradePath activePath : activePaths) {
                addStepsToMarket(activePath);
                TradeStep latestStep = activePath.getLatestStep();
                if (latestStep != null && latestStep.getStatus() == TradeStatus.ACTIVE) {
                    TradeAccount account = accountRepository.findOne(activePath.getAccountId());
                    checkStep(account, activePath, latestStep);
                }
            }
        } finally {
            scheduleLockRepository.unlock(SCHEDULE_NAME);
        }
    }

    private void checkStep(TradeAccount account, TradePath path, TradeStep step) {
        step.setDirty();
        try {
            BinanceOrderResult orderResult = orderService.getStepStatus(account, step);
            if ("FILLED".equals(orderResult.getStatus())) {
                log.info("Order " + orderResult.getOrderId() + " from path plan #" + step.getPlanId() + " is filled. Setting up next path step.");
                handleFilledOrder(account, path, step, orderResult);
            } else if("CANCELED".equals(orderResult.getStatus())) {
                log.info("Order " + orderResult.getOrderId() + " was cancelled. Cancel path plan #" + path.getPlanId());
                handleCanceledOrder(path, step, orderResult);
            } else if("PARTIALLY_FILLED".equals(orderResult.getStatus())) {
                handlePartFilledOrder(account, path, step, orderResult);
            } else if("NEW".equals(orderResult.getStatus())) {
                handleUnfilledOrder(account, path, step, orderResult);
            } else {
                log.info("Order " + orderResult.getOrderId() + " from path " + step.getPlanId() + " is in status: " + orderResult.getStatus());
            }
        } catch (BinanceException e) {
            log.info("Order " + step.getOrderId() + " from path " + step.getPlanId() + " failed to check status");
            if(step.getId() != 0) {
                TradeAuditLog.logBinanceException(step, e, "check order");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if(step.getId() != 0) {
                TradeAuditLog.logException(step, e);
            }
        }
    }

    private void handleCanceledOrder(TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        path.cancel();
        step.cancel();

        saveSubplan(path);
        planRepository.updateStatus(step.getPlanId(), TradePlanStatus.CANCELLED.name());
    }

    private void handleFilledOrder(TradeAccount account, TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        // update status and calc step in and out filling
        step.finish();

        // update lastActionDate on plan
        TradePlan plan = planRepository.findOne(path.getPlanId());
        plan.setLastActionDate(ZonedDateTime.now());

        if(step.getOutCurrency().equals(path.getDestCurrency())) {
            //Path finished
            path.finish();
            path.setDestAmount(step.getOutAmount());

            // calculate balance
            double balance;
            TradeStep firstStep = path.getFirstStep();
            if(firstStep != null) {
                balance = step.getOutAmount() - firstStep.getInFilled();
            } else {
                // no first step (was maybe deleted) -> use plan startAmount as approximate
                balance = step.getOutAmount() - path.getStartAmount();
            }
            //update plan info
            plan.addBalancePerc(balance / path.getStartAmount());
            plan.incRunsDone();

            if(path.isAutoRestart()) {
                startPath(account, new TradePath(path));
            } else {
                plan.setStatus(TradePlanStatus.FINISHED);
            }
        } else {
            startNextStep(account, path, step);
        }
        planRepository.save(plan);
        saveSubplan(path);
    }

    private void handlePartFilledOrder(TradeAccount account, TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        // is filling lower than minimum trade amount or filling equals to orderFilling? handle as unfilled
        String symbol = orderResult.getSymbol();
        double executedQty = Double.parseDouble(orderResult.getExecutedQty()) - step.getOrderFilled();
        double price = Double.parseDouble(orderResult.getPrice());
        if(step.getInFilled() == 0 && !exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), executedQty, price)) {
            handleUnfilledOrder(account, path, step, orderResult);
            return;
        }

        // is rest of filling lower than minimum trade amount? cancel rest order and handle as filled
        double origQty = Double.parseDouble(orderResult.getOrigQty());
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), (origQty - executedQty), price)) {
            if(!"CANCELED".equals(orderResult.getStatus())) {
                orderResult = orderService.cancelStep(account, step);
            }
            handleFilledOrder(account, path, step, orderResult);
            return;
        }

        log.info("Path plan #" + path.getPlanId() + " step-" + step.getStep() + " got a part fill. Keep going.");
        checkPrice(account, path, step, orderResult);
        saveSubplan(path);
    }

    private void handleUnfilledOrder(TradeAccount account, TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        if(step.getOutAmount() == 0) {
            checkRoute(account, path, step, orderResult);
        } else {
            checkPrice(account, path, step, orderResult);
        }
    }

    private void checkRoute(TradeAccount account, TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        RouteCalculator.RouteStep routeFirstStep = null;
        // check for better route if it is not the last step
        if(step.getStep() < path.getMaxSteps()) {
            RouteCalculator.Route route = findBestRoute(path, step.getInCurrency(), step.getInAmount());
            if(route != null) {
                routeFirstStep = route.steps.get(0);
            }
        }

        if(routeFirstStep != null && !routeFirstStep.ticker.getSymbol().equals(step.getSymbol())) {
            log.info("Path #" + path.getPlanId() + " found a better route to reach " + path.getDestCurrency() + " in " + (path.getMaxSteps() - path.getStepsCompleted()) + " steps.");
            BinanceOrderResult cancelResult = orderService.cancelStep(account, step);
            // check if there was a filling in meantime
            String symbol = cancelResult.getSymbol();
            double executedQty = Double.parseDouble(cancelResult.getExecutedQty()) - step.getOrderFilled();
            double price = Double.parseDouble(cancelResult.getPrice());
            if(exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), executedQty, price)) {
                // there was a meaningful filling
                log.info("Path plan #" + path.getPlanId() + " cancelled step had filling in the meantime. Handle that now.");
                handlePartFilledOrder(account, path, step, cancelResult);
                return;
            }

            step.setSymbol(routeFirstStep.ticker.getSymbol());
            step.setSide(routeFirstStep.isBuy ? "BUY" : "SELL");
            step.setOutCurrency(TradeUtil.otherCur(step.getSymbol(), step.getInCurrency()));
            step.setOutAmount(0);
            step.setPriceThreshold(routeFirstStep.tradePoint);
            step.setPrice(depthService.getGoodTradePrice(step));

            orderService.openStepOrder(account, step);
            saveSubplan(path);
        } else {
            if(routeFirstStep != null) {
                step.setPriceThreshold(routeFirstStep.tradePoint);
            }
            checkPrice(account, path, step, orderResult);
        }
    }

    private void checkPrice(TradeAccount account, TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        double goodPrice = depthService.getGoodTradePrice(step);
        if(goodPrice != step.getPrice()) {
            log.info("Order " + step.getOrderId() + " best trade price has changed from " + orderResult.getPrice() + " to " + String.format("%.8f", goodPrice) + ". Will adjust it.");
            if(!"CANCELED".equals(orderResult.getStatus())) {
                BinanceOrderResult cancelResult = orderService.cancelStep(account, step);
                // check if there was a filling in meantime
                String symbol = cancelResult.getSymbol();
                double executedQty = Double.parseDouble(cancelResult.getExecutedQty()) - step.getOrderFilled();
                double price = Double.parseDouble(cancelResult.getPrice());
                if(exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), executedQty, price)) {
                    // there was a meaningful filling
                    log.info("Path plan #" + path.getPlanId() + " cancelled step had filling in the meantime. Handle that now.");
                    handlePartFilledOrder(account, path, step, cancelResult);
                    return;
                }
            }
            // start the step again with new price
            step.setPrice(goodPrice);
            orderService.openStepOrder(account, step);
            saveSubplan(path);
        }
    }

    private void startNextStep(TradeAccount account, TradePath path, TradeStep currentStep) {
        RouteCalculator.Route route = findBestRoute(path, currentStep.getOutCurrency(), currentStep.getOutAmount());
        if(route != null) {
            path.setStatus(TradePlanStatus.ACTIVE);
            RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

            TradeStep step = createTradeStep(routeFirstStep, path.getStepsCompleted() + 1, currentStep.getOutCurrency(), currentStep.getOutAmount());
            step.setPlanId(path.getPlanId());
            if(step.getStep() == path.getMaxSteps()) {
                // give the last step a threshold to ensure profit
                double threshold;
                double minDestAmount = path.getFirstStep().getInFilled() * 1.005;
                if(TradeUtil.isBuy(step.getSide())) {
                    threshold = step.getInAmount() / minDestAmount;
                } else {
                    threshold = minDestAmount / step.getInAmount();
                }
                step.setPriceThreshold(threshold);
            }
            path.addStep(step);

            orderService.openStepOrder(account, step);
        } else {
            log.info("Cannot start next step for path plan #" + path.getPlanId() + " because there is no profitable route. PAUSE plan for now");
            path.setStatus(TradePlanStatus.PAUSED);
        }
    }

    private TradeStep createTradeStep(RouteCalculator.RouteStep routeStep, int stepNumber, String inCurrency, double inAmount) {
        TradeStep step = new TradeStep();
        step.setDirty();
        step.setStep(stepNumber);
        step.setStatus(TradeStatus.ACTIVE);
        step.setSymbol(routeStep.ticker.getSymbol());
        step.setInCurrency(inCurrency);
        step.setInAmount(inAmount);
        step.setOutCurrency(TradeUtil.otherCur(step.getSymbol(), step.getInCurrency()));
        step.setSide(routeStep.isBuy ? "BUY" : "SELL");
        step.setPriceThreshold(routeStep.tradePoint);
        step.setPrice(depthService.getGoodTradePrice(step));
        return step;
    }

    private RouteCalculator.Route findBestRoute(TradePath path, String startCurrency, double startAmount) {
        int maxSteps = path.getMaxSteps() - path.getStepsCompleted();
        List<BinanceTicker> tickers = Arrays.asList(marketService.getAllTicker());

        RouteCalculator calculator = new RouteCalculator(maxSteps, startCurrency, startAmount, path.getDestCurrency(), tickers);
        RouteCalculator.Route bestRoute = calculator.searchBestRoute();
        bestRoute.recalcWithoutActivityPenalties();
        double pathStartAmount = path.getStartAmount();
        if(path.getFirstStep() != null) {
            pathStartAmount = path.getFirstStep().getInFilled();
        }
        if(maxSteps == 1 || bestRoute.finalAmount >= pathStartAmount) {
            return bestRoute;
        }
        return null;
    }

    private TradePath loadActiveSubplan(long planId) {
        List<TradePath> paths = pathRepository.findAllByPlanIdAndStatus(planId, TradePlanStatus.ACTIVE);
        if(paths.size() > 0) {
            TradePath path = paths.get(0);
            addStepsToMarket(path);
            return path;
        }
        return null;
    }

    private void addStepsToMarket(TradePath path) {
        List<TradeStep> steps = stepRepository.findAllByPlanIdOrderByIdDesc(path.getPlanId());
        path.setSteps(steps);
    }

    private void saveSubplan(TradePath path) {
        pathRepository.save(path);
        if(path.getSteps() != null) {
            for(TradeStep step: path.getSteps()) {
                if(step.isDirty()) {
                    stepRepository.save(step);
                    if(step.getNewAuditLogs() != null) {
                        for(TradeAuditLog log: step.getNewAuditLogs()) {
                            log.setPlanId(step.getPlanId());
                            log.setStepId(step.getId());
                            auditLogRepository.save(log);
                        }
                    }
                }
            }
        }
    }
}
