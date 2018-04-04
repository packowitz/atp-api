package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.autotrade.domain.binance.BinanceTicker;
import io.pacworx.atp.exception.BinanceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class BinanceOneMarketService {
    private static final Logger log = LogManager.getLogger();
    private static final String SCHEDULE_NAME = "ONEMARKETCHECK";

    private final BinanceService binanceService;
    private final BinanceDepthService depthService;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final TradeOneMarketRepository oneMarketRepository;
    private final TradeAccountRepository accountRepository;
    private final TradePlanRepository planRepository;
    private final TradeStepRepository stepRepository;
    private final TradeAuditLogRepository auditLogRepository;
    private final TradeScheduleLockRepository scheduleLockRepository;

    @Autowired
    public BinanceOneMarketService(BinanceService binanceService,
                                   BinanceDepthService depthService,
                                   BinanceExchangeInfoService exchangeInfoService,
                                   TradeOneMarketRepository microRepository,
                                   TradeAccountRepository accountRepository,
                                   TradePlanRepository planRepository,
                                   TradeStepRepository stepRepository,
                                   TradeAuditLogRepository auditLogRepository,
                                   TradeScheduleLockRepository scheduleLockRepository) {
        this.binanceService = binanceService;
        this.depthService = depthService;
        this.exchangeInfoService = exchangeInfoService;
        this.oneMarketRepository = microRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.auditLogRepository = auditLogRepository;
        this.scheduleLockRepository = scheduleLockRepository;
    }

    public void startPlan(TradeAccount account, TradeOneMarket oneMarket) {
        startFirstStep(account, oneMarket);
        saveSubplan(oneMarket);
    }

    public void cancelPlan(TradeAccount account, TradePlan plan) {
        TradeOneMarket activePlan = loadSubplan(plan.getId());
        cancel(account, activePlan);
        saveSubplan(activePlan);
    }

    public void deletePlan(TradePlan plan) {
        oneMarketRepository.deleteAllByPlanId(plan.getId());
        stepRepository.deleteAllByPlanId(plan.getId());
        auditLogRepository.deleteAllByPlanId(plan.getId());
    }

    @Scheduled(fixedDelay = 20000)
    public void checkOrders() {
        if(!scheduleLockRepository.lock(SCHEDULE_NAME)) {
            return;
        }
        try {
            List<TradeOneMarket> activePlans = this.oneMarketRepository.findAllByStatus(TradePlanStatus.ACTIVE);
            for (TradeOneMarket activePlan : activePlans) {
                addStepsToMarket(activePlan);
                TradeAccount account = accountRepository.findOne(activePlan.getAccountId());
                //check first step first then step back
                TradeStep firstStep = activePlan.getActiveFirstStep();
                if (firstStep != null) {
                    checkStep(account, activePlan, firstStep);
                }
                TradeStep stepBack = activePlan.getActiveStepBack();
                if (stepBack != null) {
                    checkStep(account, activePlan, stepBack);
                }
                saveSubplan(activePlan);
            }
        } finally {
            scheduleLockRepository.unlock(SCHEDULE_NAME);
        }
    }

    private void checkStep(TradeAccount account, TradeOneMarket oneMarket, TradeStep step) {
        try {
            if(step.isNeedRestart()) {
                binanceService.openStepOrder(account, step);
            } else {
                BinanceOrderResult orderResult = binanceService.getStepStatus(account, step);
                if("CANCELED".equals(orderResult.getStatus())) {
                    log.info("Order " + orderResult.getOrderId() + " was cancelled. Cancel one-market plan #" + oneMarket.getPlanId());
                    cancel(account, oneMarket);
                } else if ("FILLED".equals(orderResult.getStatus())) {
                    handleFilledOrder(account, oneMarket, step, orderResult);
                } else if("PARTIALLY_FILLED".equals(orderResult.getStatus())) {
                    handlePartFilledOrder(account, oneMarket, step, orderResult);
                } else if("NEW".equals(orderResult.getStatus())) {
                    handleUnfilledOrder(account, oneMarket, step, orderResult);
                } else {
                    log.info("Order " + orderResult.getOrderId() + " from one-market plan #" + oneMarket.getPlanId() + " is in status: " + orderResult.getStatus());
                }
            }
        } catch (BinanceException e) {
            log.warn("Order " + step.getOrderId() + " from one-market plan #" + oneMarket.getPlanId() + " failed to check status");
            if(step.getId() != 0) {
                auditLogRepository.save(TradeAuditLog.logBinanceException(step, e));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            if(step.getId() != 0) {
                auditLogRepository.save(TradeAuditLog.logException(step, e));
            }
        }
    }

    private void cancel(TradeAccount account, TradeOneMarket oneMarket) {
        TradeStep firstStep = oneMarket.getActiveFirstStep();
        if(firstStep != null) {
            binanceService.cancelStep(account, firstStep);
        }
        TradeStep stepBack = oneMarket.getActiveStepBack();
        if(stepBack != null) {
            binanceService.cancelStep(account, stepBack);
        }
        oneMarket.cancel();
        planRepository.updateStatus(oneMarket.getPlanId(), TradePlanStatus.CANCELLED.name());
    }

    private void handleFilledOrder(TradeAccount account, TradeOneMarket oneMarket, TradeStep step, BinanceOrderResult orderResult) {
        // update status and calc step in and out filling
        step.finish();
        step.addInfoAuditLog("Step filled");
        binanceService.addMarketInfoAsAuditLog(step);

        // update lastActionDate on plan
        TradePlan plan = planRepository.findOne(oneMarket.getPlanId());
        plan.setLastActionDate(ZonedDateTime.now());

        if(step.getStep() == 1) {
            log.info("Plan #" + oneMarket.getPlanId() + " firstStep filled. Move traded coins to stepBack.");
            startStepBack(account, oneMarket, step, orderResult);
        } else {
            TradeStep firstStep = oneMarket.getActiveFirstStep();
            log.info("Plan #" + oneMarket.getPlanId() + " stepBack filled. Check if a firstStep still exists.");
            if(firstStep != null) {
                // cancel firstStep; restart stepBack if firstStep got filling in the meantime
                binanceService.cancelStep(account, firstStep);
                double diffAmount = firstStep.getOutAmount() - step.getInAmount();
                if(exchangeInfoService.isTradeBigEnough(step.getSymbol(), step.getOutCurrency(), diffAmount, step.getPrice())) {
                    // means that in the meantime firstStep got some filling
                    step.setInAmount(firstStep.getOutAmount());
                    binanceService.openStepOrder(account, step);
                    return;
                }
            }
            // calculate balance
            double balance;
            TradeStep latestFirstStep = oneMarket.getLatesFirstStep();
            if(latestFirstStep != null) {
                balance = step.getOutAmount() - latestFirstStep.getInFilled();
            } else {
                // no latest first step (was maybe deleted) -> use plan startAmount as approximate
                balance = step.getOutAmount() - oneMarket.getStartAmount();
            }
            oneMarket.addBalance(balance);

            //update plan info
            double balancePerc = oneMarket.getBalance() / oneMarket.getStartAmount();
            plan.setBalancePerc(balancePerc);
            plan.incRunsDone();

            // restart plan if auto restart is turned on
            if(oneMarket.isAutoRestart()) {
                log.info("Restart plan #" + oneMarket.getPlanId());
                startFirstStep(account, oneMarket);
            } else {
                oneMarket.finish();
                plan.setStatus(TradePlanStatus.FINISHED);
            }
        }
        planRepository.save(plan);
    }

    private void handlePartFilledOrder(TradeAccount account, TradeOneMarket oneMarket, TradeStep step, BinanceOrderResult orderResult) {
        // is filling lower than minimum trade amount or filling equals to orderFilling? handle as unfilled
        String symbol = orderResult.getSymbol();
        double price = Double.parseDouble(orderResult.getPrice());
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), step.getOrderFilled(), price)) {
            if(step.getStatus() == TradeStatus.ACTIVE) {
                handleUnfilledOrder(account, oneMarket, step, orderResult);
            }
            return;
        }

        // is rest of filling lower than minimum trade amount? cancel rest order and handle as filled
        double restQty = Double.parseDouble(orderResult.getOrigQty()) - step.getOrderFilled();
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), restQty, price)) {
            if(step.getStatus() == TradeStatus.ACTIVE) {
                orderResult = binanceService.cancelStep(account, step);
            }
            handleFilledOrder(account, oneMarket, step, orderResult);
            return;
        }

        step.addInfoAuditLog("Part filled", "Filled " + orderResult.getExecutedQty() + " / " + orderResult.getOrigQty() + " at " + String.format("%.8f", price));

        if(step.getStep() == 1) {
            log.info("Plan #" + oneMarket.getPlanId() + " firstStep got a part fill. Move traded coins to stepBack.");
            startStepBack(account, oneMarket, step, orderResult);
        } else {
            log.info("Plan #" + oneMarket.getPlanId() + " stepBack got a part fill. Keep going.");
            handleUnfilledOrder(account, oneMarket, step, orderResult);
        }
    }

    private void handleUnfilledOrder(TradeAccount account, TradeOneMarket oneMarket, TradeStep step, BinanceOrderResult orderResult) {
        // if step is cancelled then don't handle it (again)
        if("CANCELED".equals(orderResult.getStatus())) {
            return;
        }
        // adjust price if necessary
        double goodPrice = getGoodTradePoint(step, oneMarket.getMinProfit());
        if(Math.abs(goodPrice - step.getPrice()) >= 0.00000001 ) { //stupid double ...
            log.info("Plan #" + oneMarket.getPlanId() + (step.getStep() == 1 ? " firstStep" : " stepBack") + " price adjusting");
            step.addInfoAuditLog("Adjust price to " + String.format("%.8f", goodPrice));
            double orderFillingBeforeCancel = step.getOrderFilled();
            BinanceOrderResult cancelResult = binanceService.cancelStep(account, step);
            // check if there was a filling in meantime
            if(Math.abs(step.getOrderFilled() - orderFillingBeforeCancel) > 0.00000001) {
                log.info("Plan #" + oneMarket.getPlanId() + " cancelled step had filling in the meantime. Handle that now.");
                step.addInfoAuditLog("Received filling in meantime", "Traded " + String.format("%.8f", step.getOrderFilled() - orderFillingBeforeCancel) + " in the meantime");
                handlePartFilledOrder(account, oneMarket, step, cancelResult);
                if(step.getStatus() != TradeStatus.CANCELLED) {
                    // if cancelled step was filled or restarted by partFilling then it doesn't need to restart anymore
                    return;
                }
            }
            // start the step again with new price
            step.setPrice(goodPrice);
            binanceService.openStepOrder(account, step);
        }
    }

    private void startFirstStep(TradeAccount account, TradeOneMarket oneMarket) {
        TradeStep firstStep = createFirstStep(oneMarket);
        oneMarket.addStep(firstStep);
        firstStep.setNeedRestart(true);
        checkStep(account, oneMarket, firstStep);
    }

    private void startStepBack(TradeAccount account, TradeOneMarket oneMarket, TradeStep firstStep, BinanceOrderResult orderResult) {
        TradeStep stepBack = oneMarket.getActiveStepBack();
        if(stepBack != null) {
            stepBack.setDirty();
            log.info("Plan #" + oneMarket.getPlanId() + " add traded coins to existing stepBack.");
            // cancel stepBack then add traded coins to it, recalc priceThreshold and restart it
            binanceService.cancelStep(account, stepBack);
            double newAmount = firstStep.getOutAmount() - stepBack.getInAmount();
            double newThreshold = calcPriceThreshold(orderResult, oneMarket.getMinProfit());
            double oldAmount = stepBack.getInAmount() - stepBack.getInFilled();
            double oldThreshold = stepBack.getPriceThreshold();

            double avgThreshold = avgPriceThreshold(orderResult.getSymbol(), newAmount, newThreshold, oldAmount, oldThreshold);
            stepBack.setInAmount(firstStep.getOutAmount());
            stepBack.setPriceThreshold(avgThreshold);

            stepBack.setPrice(0d);
            stepBack.setPrice(getGoodTradePoint(stepBack, oneMarket.getMinProfit()));
        } else {
            log.info("Plan #" + oneMarket.getPlanId() + " create new stepBack.");
            stepBack = createStepBack(oneMarket, firstStep, orderResult);
            oneMarket.addStep(stepBack);
        }
        stepBack.setNeedRestart(true);
        checkStep(account, oneMarket, stepBack);
    }

    private TradeStep createFirstStep(TradeOneMarket oneMarket) {
        boolean isBuy = TradeUtil.isBaseCurrency(oneMarket.getStartCurrency());
        TradeStep step = new TradeStep();
        step.setDirty();
        step.setStep(1);
        step.setPlanId(oneMarket.getPlanId());
        step.setStatus(TradeStatus.ACTIVE);
        step.setSymbol(oneMarket.getSymbol());
        step.setSide(isBuy ? "BUY" : "SELL");
        step.setPrice(getGoodTradePoint(step, oneMarket.getMinProfit()));
        step.setInCurrency(oneMarket.getStartCurrency());
        step.setInAmount(oneMarket.getStartAmount());
        step.setOutCurrency(TradeUtil.otherCur(step.getSymbol(), step.getInCurrency()));
        return step;
    }

    private TradeStep createStepBack(TradeOneMarket oneMarket, TradeStep firstStep, BinanceOrderResult orderResult) {
        boolean isBuy = !TradeUtil.isBuy(firstStep.getSide());
        TradeStep step = new TradeStep();
        step.setDirty();
        step.setStep(2);
        step.setPlanId(oneMarket.getPlanId());
        step.setStatus(TradeStatus.ACTIVE);
        step.setSymbol(firstStep.getSymbol());
        step.setSide(isBuy ? "BUY" : "SELL");
        step.setPriceThreshold(calcPriceThreshold(orderResult, oneMarket.getMinProfit()));
        step.setPrice(getGoodTradePoint(step, oneMarket.getMinProfit()));
        step.setInCurrency(firstStep.getOutCurrency());
        step.setInAmount(firstStep.getOutAmount());
        step.setOutCurrency(TradeUtil.otherCur(step.getSymbol(), step.getInCurrency()));
        return step;
    }

    private double getGoodTradePoint(TradeStep step, double minProfit) {
        if(step.getStep() == 1) {
            // first step should be at least {minProfit} away from other side
            BinanceTicker ticker = binanceService.getTicker(step.getSymbol());
            double threshold;
            if(TradeUtil.isBuy(step.getSide())) {
                threshold = Double.parseDouble(ticker.getAskPrice());
                threshold /= (1d + minProfit);
            } else {
                threshold = Double.parseDouble(ticker.getBidPrice());
                threshold *= (1d + minProfit);
            }
            step.setPriceThreshold(threshold);
        }
        return depthService.getGoodTradePrice(step);
    }

    private double calcPriceThreshold(BinanceOrderResult orderResult, double minProfit) {
        double price = Double.parseDouble(orderResult.getPrice());
        if(TradeUtil.isBuy(orderResult.getSide())) {
            // sell it minProfit higher than bought
            price *= (1d + minProfit);
        } else {
            // buy it minProfit lower than sold
            price /= (1d + minProfit);
        }
        return exchangeInfoService.polishPrice(orderResult.getSymbol(), price);
    }

    private double avgPriceThreshold(String symbol, double amount1, double threshold1, double amount2, double threshold2) {
        double avg = ((amount1 * threshold1) + (amount2 * threshold2)) / (amount1 + amount2);
        return exchangeInfoService.polishPrice(symbol, avg);
    }

    private TradeOneMarket loadSubplan(long planId) {
        TradeOneMarket oneMarket = oneMarketRepository.findByPlanId(planId);
        addStepsToMarket(oneMarket);
        return oneMarket;
    }

    private void addStepsToMarket(TradeOneMarket oneMarket) {
        List<TradeStep> steps = stepRepository.findAllByPlanIdAndSubplanIdOrderByIdDesc(oneMarket.getPlanId(), oneMarket.getId());
        oneMarket.setSteps(steps);
    }

    private void saveSubplan(TradeOneMarket oneMarket) {
        oneMarketRepository.save(oneMarket);
        if(oneMarket.getSteps() != null) {
            for(TradeStep step: oneMarket.getSteps()) {
                if(step.isDirty()) {
                    step.setSubplanId(oneMarket.getId());
                    stepRepository.save(step);
                    if(step.getNewAuditLogs() != null) {
                        for(TradeAuditLog log: step.getNewAuditLogs()) {
                            log.setPlanId(step.getPlanId());
                            log.setSubplanId(step.getSubplanId());
                            log.setStepId(step.getId());
                            auditLogRepository.save(log);
                        }
                    }
                }
            }
        }
    }
}
