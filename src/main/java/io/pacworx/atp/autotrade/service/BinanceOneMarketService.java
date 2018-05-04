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
public class BinanceOneMarketService {
    private static final Logger log = LogManager.getLogger();
    private static final String PLAN_CHECK_SCHEDULE_NAME = "PLAN_CHECK";
    private static final String PAUSED_PLAN_CHECK_SCHEDULE_NAME = "PAUSED_PLAN_CHECK";

    private final BinanceService binanceService;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final TradeOneMarketRepository oneMarketRepository;
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
    public BinanceOneMarketService(BinanceService binanceService,
                                   BinanceExchangeInfoService exchangeInfoService,
                                   TradeOneMarketRepository microRepository,
                                   TradeAccountRepository accountRepository,
                                   TradePlanRepository planRepository,
                                   TradeStepRepository stepRepository,
                                   TradeAuditLogRepository auditLogRepository,
                                   TradeScheduleLockRepository scheduleLockRepository,
                                   TradePlanConfigRepository planConfigRepository,
                                   StrategyResolver strategyResolver) {
        this.binanceService = binanceService;
        this.exchangeInfoService = exchangeInfoService;
        this.oneMarketRepository = microRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.auditLogRepository = auditLogRepository;
        this.scheduleLockRepository = scheduleLockRepository;
        this.planConfigRepository = planConfigRepository;
        this.strategyResolver = strategyResolver;
    }

    public void startPlan(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket) {
        startFirstStep(account, plan, oneMarket);
        savePlan(plan, oneMarket);
    }

    public void cancelPlan(TradeAccount account, TradePlan plan) {
        TradeOneMarket activePlan = loadSubplan(plan.getId());
        cancel(account, plan, activePlan);
        savePlan(plan, activePlan);
    }

    public void deletePlan(TradePlan plan) {
        oneMarketRepository.deleteAllByPlanId(plan.getId());
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

    @Scheduled(fixedDelay = 60000)
    public void checkPausedPlans() {
        if(shutdownRecognized || !scheduleLockRepository.lock(PAUSED_PLAN_CHECK_SCHEDULE_NAME)) {
            return;
        }
        try {
            checkPausedPlansRunning = true;
            List<TradeOneMarket> pausedPlans = this.oneMarketRepository.findAllByStatus(TradePlanStatus.PAUSED);
            for(TradeOneMarket pausedPlan: pausedPlans) {
                if(shutdownRecognized) {
                    log.info("Shutdown recognized. Stop check paused plans");
                    break;
                }
                TradePlan plan = planRepository.findOne(pausedPlan.getPlanId());
                loadPlanConfig(plan);
                loadStepsToMarket(pausedPlan);
                //get latest cancelled step and check if there is a new market for it
                TradeStep step = pausedPlan.getLatestCancelledStep();
                MarketStrategy strategy;
                if(step.getStep() == 1) {
                    strategy = strategyResolver.resolveFirstStepStrategy(plan.getConfig().getFirstMarketStrategy());
                } else {
                    strategy = strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy());
                }
                if(strategy.checkMarket(plan, step)) {
                    String newMarket = strategy.getMarket(plan, step);
                    step.setCheckedMarketDate(ZonedDateTime.now());
                    if(newMarket != null) {
                        //restart in new market
                        log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " found a good market again: " + newMarket);
                        step.setTradingMarket(newMarket);
                        step.setNeedRestart(true);
                        step.setStatus(TradeStatus.ACTIVE);
                        plan.setStatus(TradePlanStatus.ACTIVE);
                        pausedPlan.setStatus(TradePlanStatus.ACTIVE);
                        TradeAccount account = accountRepository.findOne(pausedPlan.getAccountId());
                        checkStep(account, plan, pausedPlan, step);
                    }
                }
                savePlan(plan, pausedPlan);
            }
        } finally {
            scheduleLockRepository.unlock(PAUSED_PLAN_CHECK_SCHEDULE_NAME);
            checkPausedPlansRunning = false;
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void checkOrders() {
        if(shutdownRecognized || !scheduleLockRepository.lock(PLAN_CHECK_SCHEDULE_NAME)) {
            return;
        }
        try {
            checkOrdersRunning = true;
            List<TradeOneMarket> activePlans = this.oneMarketRepository.findAllByStatus(TradePlanStatus.ACTIVE);
            for (TradeOneMarket activePlan : activePlans) {
                if(shutdownRecognized) {
                    log.info("Shutdown recognized. Stop check orders");
                    break;
                }
                TradePlan plan = planRepository.findOne(activePlan.getPlanId());
                loadPlanConfig(plan);
                loadStepsToMarket(activePlan);
                TradeAccount account = accountRepository.findOne(activePlan.getAccountId());
                //check first step first then step back
                TradeStep firstStep = activePlan.getCurrentFirstStep();
                if (firstStep != null) {
                    checkStep(account, plan, activePlan, firstStep);
                }
                TradeStep stepBack = activePlan.getCurrentStepBack();
                if (stepBack != null) {
                    checkStep(account, plan, activePlan, stepBack);
                }
                savePlan(plan, activePlan);
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

    private void checkStep(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket, TradeStep step) {
        try {
            if(step.isNeedRestart()) {
                if(step.getOrderId() == null) {
                    marketAndPriceCheck(account, plan, oneMarket, step);
                } else {
                    BinanceOrderResult orderResult = binanceService.getStepStatus(account, step);
                    handlePartFilledOrder(account, plan, oneMarket, step, orderResult);
                }
            } else {
                BinanceOrderResult orderResult = binanceService.getStepStatus(account, step);
                if("CANCELED".equals(orderResult.getStatus())) {
                    log.info("Order " + orderResult.getOrderId() + " was cancelled. Cancel one-market plan #" + oneMarket.getPlanId());
                    cancel(account, plan, oneMarket);
                } else if ("FILLED".equals(orderResult.getStatus())) {
                    handleFilledOrder(account, plan, oneMarket, step);
                } else if("PARTIALLY_FILLED".equals(orderResult.getStatus())) {
                    handlePartFilledOrder(account, plan, oneMarket, step, orderResult);
                } else if("NEW".equals(orderResult.getStatus())) {
                    marketAndPriceCheck(account, plan, oneMarket, step);
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

    private void cancel(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket) {
        TradeStep firstStep = oneMarket.getCurrentFirstStep();
        if(firstStep != null && firstStep.getStatus() != TradeStatus.CANCELLED) {
            binanceService.cancelStepAndIgnoreStatus(account, firstStep);
        }
        TradeStep stepBack = oneMarket.getCurrentStepBack();
        if(stepBack != null && stepBack.getStatus() != TradeStatus.CANCELLED) {
            binanceService.cancelStepAndIgnoreStatus(account, stepBack);
        }
        oneMarket.cancel();
        plan.setStatus(TradePlanStatus.CANCELLED);
        plan.setFinishDate(ZonedDateTime.now());
    }

    private void handleFilledOrder(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket, TradeStep step) {
        // update status and calc step in and out filling
        step.finish();
        binanceService.addMarketInfoAsAuditLog(step);

        // update lastActionDate on plan
        plan.setLastActionDate(ZonedDateTime.now());

        if(!isLastStep(plan, step)) {
            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " filled. Move traded coins to stepBack.");
            startStepBack(account, plan, oneMarket, step);
        } else {
            TradeStep firstStep = oneMarket.getCurrentFirstStep();
            log.info("Plan #" + plan.getId() + " last step filled. Check if a previous step still exists.");
            if(firstStep != null) {
                if(firstStep.getStatus() == TradeStatus.CANCELLED) {
                    firstStep.setNeedRestart(false);
                }
                // cancel firstStep; restart stepBack if firstStep got filling in the meantime
                binanceService.cancelStepAndIgnoreStatus(account, firstStep);
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
                balance = step.getOutAmount() - plan.getConfig().getStartAmount();
            }
            oneMarket.addBalance(balance);
            plan.setBalance(oneMarket.getBalance());

            //update plan info
            double balancePerc = plan.getBalance() / plan.getConfig().getStartAmount();
            plan.setBalancePerc(balancePerc);
            plan.incRunsDone();

            // restart plan if auto restart is turned on
            if(plan.getConfig().isAutoRestart()) {
                log.info("Restart plan #" + plan.getId());
                startFirstStep(account, plan, oneMarket);
            } else {
                oneMarket.finish();
                plan.setStatus(TradePlanStatus.FINISHED);
                plan.setFinishDate(ZonedDateTime.now());
            }
        }
    }

    private void handlePartFilledOrder(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket, TradeStep step, BinanceOrderResult orderResult) {
        // is filling lower than minimum trade amount or filling equals to orderFilling? handle as unfilled
        String symbol = orderResult.getSymbol();
        double price = Double.parseDouble(orderResult.getPrice());
        double orderFilling = Double.parseDouble(orderResult.getExecutedQty());
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), orderFilling, price)) {
            marketAndPriceCheck(account, plan, oneMarket, step);
            return;
        }

        // is rest of filling lower than minimum trade amount? cancel rest order and handle as filled
        double restQty = Double.parseDouble(orderResult.getOrigQty()) - orderFilling;
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), restQty, price)) {
            if(step.getStatus() == TradeStatus.ACTIVE) {
                binanceService.cancelStepAndIgnoreStatus(account, step);
            }
            handleFilledOrder(account, plan, oneMarket, step);
            return;
        }

        if(step.getNewFilling() > 0d) {
            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " got a part fill of " + String.format("%.8f", step.getNewFilling()) + ". Keep going.");
            if(!isLastStep(plan, step) && strategyResolver.resolveNextStepStrategy(plan.getConfig().getNextMarketStrategy()).allowPartialNextStep()) {
                startStepBack(account, plan, oneMarket, step);
            }
        }
        marketAndPriceCheck(account, plan, oneMarket, step);
    }

    private void marketAndPriceCheck(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket, TradeStep step) {
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
                        binanceService.cancelStepAndRestartOnError(account, step);
                    }
                    if(step.getOrderFilled() > 0.00000001) {
                        //There was filling in the meantime. restart step
                        step.setNeedRestart(true);
                    } else {
                        if(newMarket == null) {
                            //no good trading market found atm -> pause plan
                            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " found no good market. Pause plan.");
                            oneMarket.setStatus(TradePlanStatus.PAUSED);
                            plan.setStatus(TradePlanStatus.PAUSED);
                            return;
                        } else {
                            //swap to new market
                            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " found new good market: " + newMarket);
                            step.setTradingMarket(newMarket);
                            step.setSide(TradeUtil.getSideOfMarket(newMarket, step.getInCurrency()));
                            step.setNeedRestart(true);
                        }
                    }
                }
            }
        }

        // adjust price if necessary
        double goodPrice = getGoodTradePoint(plan, step);
        if(step.isNeedRestart() || Math.abs(goodPrice - step.getPrice()) >= 0.00000001 ) { //stupid double ...
            log.info("Plan #" + plan.getId() + " step-" + step.getStep() + " price adjusting");
            step.addInfoAuditLog("Adjust price to " + String.format("%.8f", goodPrice));

            if(step.getStatus() != TradeStatus.CANCELLED && step.getOrderId() != null) {
                double orderFillingBeforeCancel = step.getOrderFilled();
                BinanceOrderResult cancelResult = binanceService.cancelStepAndRestartOnError(account, step);
                // check if there was a filling in meantime
                if(Math.abs(step.getOrderFilled() - orderFillingBeforeCancel) > 0.00000001) {
                    log.info("Plan #" + plan.getId() + " cancelled step had filling in the meantime. Handle that now.");
                    step.addInfoAuditLog("Received filling in meantime", "Traded " + String.format("%.8f", step.getOrderFilled() - orderFillingBeforeCancel) + " in the meantime");
                    handlePartFilledOrder(account, plan, oneMarket, step, cancelResult);
                    if(step.getStatus() != TradeStatus.CANCELLED) {
                        // if cancelled step was filled or restarted by partFilling then it doesn't need to restart anymore
                        return;
                    }
                }
            }

            // start the step again with new price
            step.setPrice(goodPrice);
            binanceService.openStepOrder(account, step);
        }
    }

    private void startFirstStep(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket) {
        TradeStep firstStep = createStep(plan, null);
        oneMarket.addStep(firstStep);
        if(firstStep.getSymbol() != null) {
            firstStep.setNeedRestart(true);
            checkStep(account, plan, oneMarket, firstStep);
        } else {
            firstStep.setStatus(TradeStatus.CANCELLED);
            plan.setStatus(TradePlanStatus.PAUSED);
            oneMarket.setStatus(TradePlanStatus.PAUSED);
        }
    }

    private void startStepBack(TradeAccount account, TradePlan plan, TradeOneMarket oneMarket, TradeStep prevStep) {
        TradeStep stepBack = oneMarket.getCurrentStepBack();
        if(stepBack != null) {
            stepBack.setDirty();
            log.info("Plan #" + plan.getId() + " add traded coins to existing step-" + stepBack.getStep());
            // cancel stepBack then add traded coins to it, recalc priceThreshold and restart it
            binanceService.cancelStepAndIgnoreStatus(account, stepBack);

            setThreshold(plan, stepBack, prevStep);
            stepBack.setPrice(0d);
            stepBack.setPrice(getGoodTradePoint(plan, stepBack));
            stepBack.setInAmount(prevStep.getOutAmount());
        } else {
            log.info("Plan #" + plan.getId() + " create new step" + (prevStep.getStep() + 1));
            stepBack = createStep(plan, prevStep);
            oneMarket.addStep(stepBack);
        }
        if(stepBack.getSymbol() != null) {
            stepBack.setNeedRestart(true);
            checkStep(account, plan, oneMarket, stepBack);
        } else {
            stepBack.setStatus(TradeStatus.CANCELLED);
            plan.setStatus(TradePlanStatus.PAUSED);
            oneMarket.setStatus(TradePlanStatus.PAUSED);
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
        } else {
            step.setStatus(TradeStatus.CANCELLED);
        }
        setThreshold(plan, step, prevStep);
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

    private TradeOneMarket loadSubplan(long planId) {
        TradeOneMarket oneMarket = oneMarketRepository.findByPlanId(planId);
        loadStepsToMarket(oneMarket);
        return oneMarket;
    }

    private void loadStepsToMarket(TradeOneMarket oneMarket) {
        List<TradeStep> steps = stepRepository.findAllByPlanIdAndSubplanIdOrderByIdDesc(oneMarket.getPlanId(), oneMarket.getId());
        oneMarket.setSteps(steps);
    }

    private void savePlan(TradePlan plan, TradeOneMarket oneMarket) {
        planRepository.save(plan);
        oneMarketRepository.save(oneMarket);
        if(oneMarket.getSteps() != null) {
            for(TradeStep step: oneMarket.getSteps()) {
                if(step.isDirty()) {
                    step.setSubplanId(oneMarket.getId());
                    stepRepository.save(step);
                    if(step.getNewAuditLogs() != null) {
                        for(TradeAuditLog log: step.getNewAuditLogs()) {
                            log.setPlanId(plan.getId());
                            log.setSubplanId(oneMarket.getId());
                            log.setStepId(step.getId());
                            auditLogRepository.save(log);
                        }
                    }
                }
            }
        }
    }
}
