package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.autotrade.service.strategies.MarketStrategy;
import io.pacworx.atp.autotrade.service.strategies.PriceStrategy;
import io.pacworx.atp.autotrade.service.strategies.StrategyResolver;
import io.pacworx.atp.exception.BinanceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class BinancePlanService {
    private static final Logger log = LogManager.getLogger();
    private static final String PLAN_CHECK_SCHEDULE_NAME = "PLAN_CHECK";
    private static final String PAUSED_PLAN_CHECK_SCHEDULE_NAME = "PAUSED_PLAN_CHECK";

    private final BinanceOrderService orderService;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final TradeAccountRepository accountRepository;
    private final TradePlanRepository planRepository;
    private final TradeStepRepository stepRepository;
    private final TradeAuditLogRepository auditLogRepository;
    private final TradeScheduleLockRepository scheduleLockRepository;
    private final TradePlanConfigRepository planConfigRepository;
    private final StrategyResolver strategyResolver;

    private boolean shutdownRecognized = false;
    private boolean checkOrdersRunning = false;
    private boolean checkPausedPlansRunning = false;

    @Autowired
    public BinancePlanService(BinanceOrderService orderService,
                              BinanceExchangeInfoService exchangeInfoService,
                              TradeAccountRepository accountRepository,
                              TradePlanRepository planRepository,
                              TradeStepRepository stepRepository,
                              TradeAuditLogRepository auditLogRepository,
                              TradeScheduleLockRepository scheduleLockRepository,
                              TradePlanConfigRepository planConfigRepository,
                              StrategyResolver strategyResolver) {
        this.orderService = orderService;
        this.exchangeInfoService = exchangeInfoService;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.auditLogRepository = auditLogRepository;
        this.scheduleLockRepository = scheduleLockRepository;
        this.planConfigRepository = planConfigRepository;
        this.strategyResolver = strategyResolver;
    }

    public void startPlan(TradeAccount account, TradePlan plan) {
        startFirstStep(account, plan);
        plan.setLastActionDate(ZonedDateTime.now());
        savePlan(plan);
    }

    public void cancelPlan(TradeAccount account, TradePlan plan) {
        loadPlanConfig(plan);
        loadStepsToPlan(plan);
        cancel(account, plan);
        savePlan(plan);
    }

    public void deletePlan(TradePlan plan) {
        planRepository.delete(plan);
        planConfigRepository.deleteAllByPlanId(plan.getId());
        stepRepository.deleteAllByPlanId(plan.getId());
        auditLogRepository.deleteAllByPlanId(plan.getId());
    }

    @PreDestroy
    public void preDestroy() throws Exception {
        shutdownRecognized = true;
        while(checkOrdersRunning || checkPausedPlansRunning) {
            System.out.println("A scheduler is running. Wait for a graceful shutdown.");
            TimeUnit.MILLISECONDS.sleep(50);
        }
        System.out.println("No scheduler running anymore.");
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void checkPausedPlans() {
        if(shutdownRecognized || !scheduleLockRepository.lock(PAUSED_PLAN_CHECK_SCHEDULE_NAME)) {
            return;
        }
        try {
            checkPausedPlansRunning = true;
            List<TradePlan> pausedPlans = this.planRepository.findAllByStatus(TradePlanStatus.PAUSED);
            for(TradePlan pausedPlan: pausedPlans) {
                if(shutdownRecognized) {
                    log.info("Shutdown recognized. Stop check paused plans");
                    break;
                }
                loadPlanConfig(pausedPlan);
                loadStepsToPlan(pausedPlan);
                //get latest cancelled step and check if there is a new market for it
                TradeStep step = pausedPlan.getLatestCancelledStep();
                MarketStrategy strategy;
                if(step.getStep() == 1) {
                    strategy = strategyResolver.resolveFirstStepStrategy(pausedPlan.getConfig().getFirstMarketStrategy());
                } else {
                    strategy = strategyResolver.resolveNextStepStrategy(pausedPlan.getConfig().getNextMarketStrategy());
                }
                if(strategy.checkMarket(pausedPlan, step)) {
                    String newMarket = strategy.getMarket(pausedPlan, step);
                    step.setCheckedMarketDate(ZonedDateTime.now());
                    if(newMarket != null) {
                        //restart in new market
                        log.info("Plan #" + pausedPlan.getId() + " step-" + step.getStep() + " found a good market again: " + newMarket);
                        step.addInfoAuditLog("Found good market again " + newMarket);
                        step.setTradingMarket(newMarket);
                        step.setNeedRestart(true);
                        pausedPlan.setStatus(TradePlanStatus.ACTIVE);
                        TradeAccount account = accountRepository.findOne(pausedPlan.getAccountId());
                        checkStep(account, pausedPlan, step);
                    }
                }
                savePlan(pausedPlan);
            }
        } finally {
            scheduleLockRepository.unlock(PAUSED_PLAN_CHECK_SCHEDULE_NAME);
            checkPausedPlansRunning = false;
        }
    }

    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void checkOrders() {
        if(shutdownRecognized || !scheduleLockRepository.lock(PLAN_CHECK_SCHEDULE_NAME)) {
            return;
        }
        try {
            checkOrdersRunning = true;
            List<TradePlan> activePlans = this.planRepository.findAllByStatus(TradePlanStatus.ACTIVE);
            for (TradePlan activePlan : activePlans) {
                if(shutdownRecognized) {
                    log.info("Shutdown recognized. Stop check orders");
                    break;
                }
                loadPlanConfig(activePlan);
                loadStepsToPlan(activePlan);
                TradeAccount account = accountRepository.findOne(activePlan.getAccountId());

                //check first steps first then later steps
                List<TradeStep> steps = activePlan.getActiveSteps();
                if(steps != null && !steps.isEmpty()) {
                    for(int i = steps.size() - 1; i >= 0; i--) {
                        checkStep(account, activePlan, steps.get(i));
                    }
                }
                savePlan(activePlan);
            }
        } finally {
            scheduleLockRepository.unlock(PLAN_CHECK_SCHEDULE_NAME);
            checkOrdersRunning = false;
        }
    }

    private void loadPlanConfig(TradePlan plan) {
        TradePlanConfig config = planConfigRepository.findOne(plan.getId());
        if(config == null) {
            log.error("Couldn't find config for plan #" + plan.getId());
        }
        plan.setConfig(config);
    }

    private void checkStep(TradeAccount account, TradePlan plan, TradeStep step) {
        try {
            if(step.isNeedRestart()) {
                if(step.getOrderId() == null || step.getStatus() == TradeStatus.CANCELLED) {
                    marketAndPriceCheck(account, plan, step);
                } else {
                    BinanceOrderResult orderResult = orderService.getStepStatus(account, step);
                    handlePartFilledOrder(account, plan, step, orderResult);
                }
            } else {
                BinanceOrderResult orderResult = orderService.getStepStatus(account, step);
                if("CANCELED".equals(orderResult.getStatus())) {
                    log.info("Order " + orderResult.getOrderId() + " was cancelled. Cancel one-market plan #" + plan.getId());
                    cancel(account, plan);
                } else if ("FILLED".equals(orderResult.getStatus())) {
                    handleFilledOrder(account, plan, step);
                } else if("PARTIALLY_FILLED".equals(orderResult.getStatus())) {
                    handlePartFilledOrder(account, plan, step, orderResult);
                } else if("NEW".equals(orderResult.getStatus())) {
                    marketAndPriceCheck(account, plan, step);
                } else {
                    log.info("Order " + orderResult.getOrderId() + " from one-market plan #" + plan.getId() + " is in status: " + orderResult.getStatus());
                }
            }
        } catch (BinanceException e) {
            log.warn("Order " + step.getOrderId() + " from one-market plan #" + plan.getId() + " failed to check status");
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

    private void cancel(TradeAccount account, TradePlan plan) {
        for(TradeStep step: plan.getActiveSteps()) {
            if(step.getStatus() == TradeStatus.ACTIVE && step.getOrderId() != null) {
                orderService.cancelStepAndIgnoreStatus(account, step);
            }
        }
        plan.cancel();
    }

    private void handleFilledOrder(TradeAccount account, TradePlan plan, TradeStep step) {
        // update status and calc step in and out filling
        step.finish();
        orderService.addMarketInfoAsAuditLog(step);

        // update lastActionDate on plan
        plan.setLastActionDate(ZonedDateTime.now());

        if(!isLastStep(plan, step)) {
            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " filled. Move traded coins to stepBack.");
            startStepBack(account, plan, step);
        } else {
            TradeStep firstStep = plan.getActiveFirstStep();
            log.info("Plan #" + plan.getId() + " last step filled. Check if a previous step still exists.");
            if(firstStep != null) {
                if(firstStep.getStatus() == TradeStatus.CANCELLED) {
                    firstStep.setNeedRestart(false);
                }
                // cancel firstStep; restart stepBack if firstStep got filling in the meantime
                orderService.cancelStepAndIgnoreStatus(account, firstStep);
                double diffAmount = firstStep.getOutAmount() - step.getInAmount();
                if(exchangeInfoService.isTradeBigEnough(step.getSymbol(), step.getOutCurrency(), diffAmount, step.getPrice())) {
                    // means that in the meantime firstStep got some filling
                    step.setInAmount(firstStep.getOutAmount());
                    orderService.openStepOrder(account, step);
                    return;
                }
            }
            // calculate balance
            double balance;
            TradeStep latestFirstStep = plan.getLatestFirstStep();
            if(latestFirstStep != null) {
                balance = step.getOutAmount() - latestFirstStep.getInFilled();
            } else {
                // no latest first step (was maybe deleted) -> use plan startAmount as approximate
                balance = step.getOutAmount() - plan.getConfig().getStartAmount();
            }
            plan.addBalance(balance);

            //update plan info
            double balancePerc = plan.getBalance() / plan.getConfig().getStartAmount();
            plan.setBalancePerc(balancePerc);
            plan.incRunsDone();

            // restart plan if auto restart is turned on
            if(plan.getConfig().isAutoRestart()) {
                log.info("Restart plan #" + plan.getId());
                startFirstStep(account, plan);
            } else {
                plan.finish();
            }
        }
    }

    private void handlePartFilledOrder(TradeAccount account, TradePlan plan, TradeStep step, BinanceOrderResult orderResult) {
        // is filling lower than minimum trade amount or filling equals to orderFilling? handle as unfilled
        String symbol = orderResult.getSymbol();
        double price = Double.parseDouble(orderResult.getPrice());
        double orderFilling = Double.parseDouble(orderResult.getExecutedQty());
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), orderFilling, price)) {
            marketAndPriceCheck(account, plan, step);
            return;
        }

        // is rest of filling lower than minimum trade amount? cancel rest order and handle as filled
        double restQty = Double.parseDouble(orderResult.getOrigQty()) - orderFilling;
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), restQty, price)) {
            if(step.getStatus() == TradeStatus.ACTIVE) {
                orderService.cancelStepAndIgnoreStatus(account, step);
            }
            handleFilledOrder(account, plan, step);
            return;
        }

        if(step.getNewFilling() > 0d) {
            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " got a part fill of " + String.format("%.8f", step.getNewFilling()) + ". Keep going.");
            if(!isLastStep(plan, step) && strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy()).allowPartialNextStep()) {
                startStepBack(account, plan, step);
            }
        }
        marketAndPriceCheck(account, plan, step);
    }

    private void marketAndPriceCheck(TradeAccount account, TradePlan plan, TradeStep step) {
        // if step is cancelled then don't handle it (again)
        if(!step.isNeedRestart() && step.getStatus() == TradeStatus.CANCELLED) {
            return;
        }
        //check if the market is still good if there was no trading yet on this step
        if(step.getInFilled() < 0.00000001) {
            MarketStrategy strategy;
            if(step.getStep() == 1) {
                strategy = strategyResolver.resolveFirstStepStrategy(plan.getConfig().getFirstMarketStrategy());
            } else {
                strategy = strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy());
            }
            if(strategy.checkMarket(plan, step) || step.getSymbol() == null) {
                String newMarket = strategy.getMarket(plan, step);
                step.setCheckedMarketDate(ZonedDateTime.now());
                if(newMarket == null || !newMarket.equals(step.getSymbol())) {
                    if(step.getStatus() == TradeStatus.ACTIVE && step.getOrderId() != null) {
                        orderService.cancelStepAndRestartOnError(account, step);
                    }
                    if(step.getOrderFilled() > 0.00000001) {
                        //There was filling in the meantime. restart step
                        step.setNeedRestart(true);
                    } else {
                        if(newMarket == null) {
                            //no good trading market found atm -> pause plan
                            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " found no good market. Pause plan.");
                            step.addInfoAuditLog("Found no good market -> PAUSE ");
                            plan.setStatus(TradePlanStatus.PAUSED);
                            return;
                        } else {
                            //swap to new market
                            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " found new good market: " + newMarket);
                            step.addInfoAuditLog("Found better market " + newMarket);
                            step.setTradingMarket(newMarket);
                            step.setNeedRestart(true);
                        }
                    }
                }
            }
        }

        //Check for threshold
        PriceStrategy priceStrategy;
        if(step.getStep() == 1) {
            priceStrategy = strategyResolver.resolveFirstStepPriceStrategy(plan.getConfig().getFirstStepPriceStrategy());
        } else {
            priceStrategy = strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy());
        }
        if(priceStrategy.isThresholdDynamic()) {
            TradeStep prevStep = null;
            if(step.getStep() != 1) {
                prevStep = plan.getLatestFirstStep();
            }
            setThreshold(plan, step, prevStep);
        }

        // adjust price if necessary
        double goodPrice = getGoodTradePoint(plan, step);
        if(step.isNeedRestart() || Math.abs(goodPrice - step.getPrice()) >= 0.00000001 ) { //stupid double ...
            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " price adjusting");
            step.addInfoAuditLog("Adjust price to " + String.format("%.8f", goodPrice));

            if(step.getStatus() != TradeStatus.CANCELLED && step.getOrderId() != null) {
                double orderFillingBeforeCancel = step.getOrderFilled();
                BinanceOrderResult cancelResult = orderService.cancelStepAndRestartOnError(account, step);
                // check if there was a filling in meantime
                if(Math.abs(step.getOrderFilled() - orderFillingBeforeCancel) > 0.00000001) {
                    log.info("Plan #" + plan.getId() + " cancelled step had filling in the meantime. Handle that now.");
                    step.addInfoAuditLog("Received filling in meantime", "Traded " + String.format("%.8f", step.getOrderFilled() - orderFillingBeforeCancel) + " in the meantime");
                    handlePartFilledOrder(account, plan, step, cancelResult);
                    if(step.getStatus() != TradeStatus.CANCELLED) {
                        // if cancelled step was filled or restarted by partFilling then it doesn't need to restart anymore
                        return;
                    }
                }
            }

            // start the step again with new price
            step.setPrice(goodPrice);
            orderService.openStepOrder(account, step);
        }
    }

    private void startFirstStep(TradeAccount account, TradePlan plan) {
        TradeStep firstStep = createStep(plan, null);
        plan.addStep(firstStep);
        if(firstStep.getSymbol() != null) {
            firstStep.setNeedRestart(true);
            checkStep(account, plan, firstStep);
        } else {
            firstStep.setStatus(TradeStatus.CANCELLED);
            plan.setStatus(TradePlanStatus.PAUSED);
        }
    }

    private void startStepBack(TradeAccount account, TradePlan plan, TradeStep prevStep) {
        TradeStep stepBack = plan.getActiveStep(prevStep.getStep() + 1);
        if(stepBack != null) {
            stepBack.setDirty();
            log.info("Plan #" + plan.getId() + " add traded coins to existing step-" + stepBack.getStep());
            // cancel stepBack then add traded coins to it, recalc priceThreshold and restart it
            orderService.cancelStepAndIgnoreStatus(account, stepBack);

            setThreshold(plan, stepBack, prevStep);
            stepBack.setPrice(0d);
            stepBack.setPrice(getGoodTradePoint(plan, stepBack));
            stepBack.setInAmount(prevStep.getOutAmount());
        } else {
            log.info("Plan #" + plan.getId() + " create new step" + (prevStep.getStep() + 1));
            stepBack = createStep(plan, prevStep);
            plan.addStep(stepBack);
        }
        if(stepBack.getSymbol() != null) {
            stepBack.setNeedRestart(true);
            checkStep(account, plan, stepBack);
        } else {
            stepBack.setStatus(TradeStatus.CANCELLED);
            plan.setStatus(TradePlanStatus.PAUSED);
        }
    }

    private TradeStep createStep(TradePlan plan, TradeStep prevStep) {
        TradeStep step = new TradeStep();
        String symbol;
        if(prevStep != null) {
            symbol = strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy()).getMarket(plan, prevStep);
            step.setStep(prevStep.getStep() + 1);
            step.setInCurrency(prevStep.getOutCurrency());
            step.setInAmount(prevStep.getOutAmount());
        } else {
            symbol = strategyResolver.resolveFirstStepStrategy(plan.getConfig().getFirstMarketStrategy()).getMarket(plan);
            step.setStep(1);
            step.setInCurrency(plan.getConfig().getStartCurrency());
            step.setInAmount(plan.getConfig().getStartAmount());
        }
        step.setDirty();
        step.setPlanId(plan.getId());
        step.setCheckedMarketDate(ZonedDateTime.now());
        step.setPrice(0d);
        if(symbol != null) {
            step.setTradingMarket(symbol);
            step.setStatus(TradeStatus.ACTIVE);
            setThreshold(plan, step, prevStep);
        } else {
            step.setStatus(TradeStatus.CANCELLED);
        }
        return step;
    }

    private double getGoodTradePoint(TradePlan plan, TradeStep step) {
        PriceStrategy priceStrategy;
        if(step.getStep() == 1) {
            priceStrategy = strategyResolver.resolveFirstStepPriceStrategy(plan.getConfig().getFirstStepPriceStrategy());
        } else {
            priceStrategy = strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy());
        }
        return priceStrategy.getPrice(plan, step);
    }

    private void setThreshold(TradePlan plan, TradeStep step, TradeStep prevStep) {
        PriceStrategy priceStrategy;
        if(step.getStep() == 1) {
            priceStrategy = strategyResolver.resolveFirstStepPriceStrategy(plan.getConfig().getFirstStepPriceStrategy());
        } else {
            priceStrategy = strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy());
        }
        priceStrategy.setThresholdToStep(plan, step, prevStep);
    }

    private boolean isLastStep(TradePlan plan, TradeStep step) {
        return plan.getConfig().getStartCurrency().equals(step.getOutCurrency());
    }

    private void loadStepsToPlan(TradePlan plan) {
        List<TradeStep> steps = stepRepository.findAllByPlanIdOrderByIdDesc(plan.getId());
        plan.setSteps(steps);
    }

    private void savePlan(TradePlan plan) {
        planRepository.save(plan);
        if(plan.getSteps() != null) {
            for(TradeStep step: plan.getSteps()) {
                if(step.isDirty()) {
                    stepRepository.save(step);
                    if(step.getNewAuditLogs() != null) {
                        for(TradeAuditLog log: step.getNewAuditLogs()) {
                            log.setPlanId(plan.getId());
                            log.setStepId(step.getId());
                            auditLogRepository.save(log);
                        }
                    }
                }
            }
        }
    }
}
