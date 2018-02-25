package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.exception.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class BinancePathService {
    private static final Logger log = LogManager.getLogger();

    private final BinanceService binanceService;
    private final BinanceDepthService depthService;
    private final TradePathRepository pathRepository;
    private final TradeAccountRepository accountRepository;
    private final TradePlanRepository planRepository;
    private final TradeStepRepository stepRepository;

    @Autowired
    public BinancePathService(BinanceService binanceService,
                              BinanceDepthService depthService,
                              TradePathRepository pathRepository,
                              TradeAccountRepository accountRepository,
                              TradePlanRepository planRepository,
                              TradeStepRepository stepRepository) {
        this.binanceService = binanceService;
        this.depthService = depthService;
        this.pathRepository = pathRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
    }

    public void startPath(TradeAccount account, TradePath path) {
        RouteCalculator.Route route = findBestRoute(path.getMaxSteps(), path.getStartCurrency(), path.getDestCurrency());
        RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

        TradeStep firstStep = createTradeStep(routeFirstStep, 1, path.getStartCurrency(), path.getStartAmount());
        path.addStep(firstStep);

        binanceService.openStepOrder(account, firstStep);
        saveSubplan(path);
    }

    public void cancelPaths(TradeAccount account, TradePlan plan) {
        List<TradePath> paths = pathRepository.findAllByPlanIdAndStatus(plan.getId(), TradePlanStatus.ACTIVE);
        for(TradePath path: paths) {
            addStepsToMarket(path);
            path.setStatus(TradePlanStatus.CANCELLED);
            path.setFinishDate(ZonedDateTime.now());
            TradeStep latestStep = path.getLatestStep();
            if(latestStep != null) {
                if(latestStep.getStatus() == TradeStatus.ACTIVE) {
                    binanceService.cancelOrder(account, latestStep.getSymbol(), latestStep.getOrderId());
                }
                latestStep.setStatus(TradeStatus.CANCELLED);
            }
            saveSubplan(path);
        }
        paths = pathRepository.findAllByPlanIdAndStatus(plan.getId(), TradePlanStatus.PAUSED);
        for(TradePath path: paths) {
            path.setStatus(TradePlanStatus.CANCELLED);
            path.setFinishDate(ZonedDateTime.now());
            saveSubplan(path);
        }
    }

    public void deletePaths(TradePlan plan) {
        pathRepository.deleteAllByPlanId(plan.getId());
        stepRepository.deleteAllByPlanId(plan.getId());
    }

    @Scheduled(fixedDelay = 20000)
    public void checkOrders() {
        List<TradePath> activePaths = pathRepository.findAllByStatus(TradePlanStatus.ACTIVE);
        for(TradePath activePath: activePaths) {
            addStepsToMarket(activePath);
            TradeStep latestStep = activePath.getLatestStep();
            if(latestStep != null && latestStep.getStatus() == TradeStatus.ACTIVE) {
                TradeAccount account = accountRepository.findOne(activePath.getAccountId());
                checkStep(account, activePath, latestStep);
            }
        }
    }

    private void checkStep(TradeAccount account, TradePath path, TradeStep step) {
        step.setDirty();
        try {
            BinanceOrderResult orderResult = binanceService.getOrderStatus(account, step.getSymbol(), step.getOrderId());
            if ("FILLED".equals(orderResult.getStatus())) {
                log.info("Order " + orderResult.getOrderId() + " from path plan #" + step.getPlanId() + " is filled. Setting up next path step.");
                startNextStep(account, path, step, orderResult);
            } else if("CANCELED".equals(orderResult.getStatus())) {
                log.info("Order " + orderResult.getOrderId() + " was cancelled. Deleting observer.");
                planRepository.updateStatus(step.getPlanId(), TradePlanStatus.CANCELLED.name());
                path.setStatus(TradePlanStatus.CANCELLED);
                path.setFinishDate(ZonedDateTime.now());
                step.setStatus(TradeStatus.CANCELLED);
                saveSubplan(path);
            } else if("PARTIALLY_FILLED".equals(orderResult.getStatus())) {
                double percExecuted = 100d * Double.parseDouble(orderResult.getExecutedQty()) / Double.parseDouble(orderResult.getOrigQty());
                if(percExecuted >= 99) {
                    log.info("Order " + orderResult.getOrderId() + " from path plan #" + step.getPlanId() + " is filled over 99%. Setting up next path step.");
                    binanceService.cancelOrder(account, step.getSymbol(), step.getOrderId());
                    startNextStep(account, path, step, orderResult);
                } else {
                    log.info("Order " + orderResult.getOrderId() + " is partially filled under 99%.");
                }
            } else if("NEW".equals(orderResult.getStatus())) {
                checkRoute(account, path, step, orderResult);
            } else {
                log.info("Order " + orderResult.getOrderId() + " from path " + step.getSubplanId() + " is in status: " + orderResult.getStatus());
            }
        } catch (BadRequestException e) {
            log.info("Order " + step.getOrderId() + " from path " + step.getSubplanId() + " failed to check status");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void checkRoute(TradeAccount account, TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        RouteCalculator.Route route = findBestRoute(path.getMaxSteps() - path.getStepsCompleted(), step.getInCurrency(), path.getDestCurrency());
        RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

        if(!routeFirstStep.ticker.getSymbol().equals(step.getSymbol())) {
            log.info("Path #" + path.getPlanId() + " found a better route to reach " + path.getDestCurrency() + " in " + (path.getMaxSteps() - path.getStepsCompleted()) + " steps.");
            binanceService.cancelOrder(account, step.getSymbol(), step.getOrderId());
            step.setStatus(TradeStatus.CANCELLED);

            TradeStep newStep = createTradeStep(routeFirstStep, path.getStepsCompleted() + 1, step.getInCurrency(), step.getInAmount());
            path.addStep(step);

            BinanceOrderResult result = binanceService.openStepOrder(account, newStep);

            newStep.setSymbol(step.getSymbol());
            saveSubplan(path);
        } else {
            checkPrice(account, path, step, orderResult);
        }
    }

    private void checkPrice(TradeAccount account, TradePath path, TradeStep step, BinanceOrderResult orderResult) {
        double price;
        if(TradeUtil.isBuy(orderResult.getSide())) {
            price = depthService.getGoodBuyPoint(step.getSymbol(), Double.parseDouble(orderResult.getPrice()));
        } else {
            price = depthService.getGoodSellPoint(step.getSymbol(), Double.parseDouble(orderResult.getPrice()));
        }
        if(price != Double.parseDouble(orderResult.getPrice())) {
            log.info("Order " + step.getOrderId() + " best trade price has changed from " + orderResult.getPrice() + " to " + String.format("%.8f", price) + ". Will adjust it.");
            binanceService.cancelOrder(account, step.getSymbol(), step.getOrderId());

            step.setPrice(price);
            binanceService.openStepOrder(account, step);
            saveSubplan(path);
        }
    }

    private void startNextStep(TradeAccount account, TradePath path, TradeStep currentStep, BinanceOrderResult orderResult) {
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
            path.setStatus(TradePlanStatus.FINISHED);
            path.setFinishDate(ZonedDateTime.now());
            path.setDestAmount(currentStep.getOutAmount());
            saveSubplan(path);

            if(path.isAutoRestart()) {
                startPath(account, new TradePath(path));
            } else {
                planRepository.updateStatus(path.getPlanId(), TradePlanStatus.FINISHED.name());
            }
        } else {
            saveSubplan(path);
            RouteCalculator.Route route = findBestRoute(path.getMaxSteps() - path.getStepsCompleted(), currentStep.getOutCurrency(), path.getDestCurrency());
            RouteCalculator.RouteStep routeFirstStep = route.steps.get(0);

            TradeStep step = createTradeStep(routeFirstStep, path.getStepsCompleted() + 1, currentStep.getOutCurrency(), currentStep.getOutAmount());
            path.addStep(step);

            binanceService.openStepOrder(account, step);
            step.setSymbol(step.getSymbol());
        }
        saveSubplan(path);
    }

    private TradeStep createTradeStep(RouteCalculator.RouteStep routeStep, int stepNumber, String inCurrency, double inAmount) {
        TradeStep step = new TradeStep();
        step.setDirty();
        step.setStep(stepNumber);
        step.setStatus(TradeStatus.IDLE);
        step.setSymbol(routeStep.ticker.getSymbol());
        if(routeStep.isBuy) {
            step.setSide("BUY");
            step.setPrice(depthService.getGoodBuyPoint(step.getSymbol(), 0d));
        } else {
            step.setSide("SELL");
            step.setPrice(depthService.getGoodSellPoint(step.getSymbol(), 0d));
        }
        step.setInCurrency(inCurrency);
        step.setInAmount(inAmount);
        step.setOutCurrency(TradeUtil.otherCur(step.getSymbol(), step.getInCurrency()));
        return step;
    }

    private RouteCalculator.Route findBestRoute(int maxSteps, String startCur, String destCur) {
        List<BinanceTicker> tickers = Arrays.asList(binanceService.getAllTicker());
        RouteCalculator calculator = new RouteCalculator(maxSteps, startCur, destCur, tickers);
        return calculator.searchBestRoute();
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
        List<TradeStep> steps = stepRepository.findAllByPlanIdAndSubplanIdOrderByIdDesc(path.getPlanId(), path.getId());
        path.setSteps(steps);
    }

    private void saveSubplan(TradePath path) {
        pathRepository.save(path);
        if(path.getSteps() != null) {
            for(TradeStep step: path.getSteps()) {
                if(step.isDirty()) {
                    step.setSubplanId(path.getId());
                    stepRepository.save(step);
                }
            }
        }
    }
}
