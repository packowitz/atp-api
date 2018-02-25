package io.pacworx.atp.autotrade.service;

import io.pacworx.atp.autotrade.domain.*;
import io.pacworx.atp.autotrade.domain.binance.BinanceOrderResult;
import io.pacworx.atp.exception.BadRequestException;
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

    private final BinanceService binanceService;
    private final BinanceDepthService depthService;
    private final BinanceExchangeInfoService exchangeInfoService;
    private final TradeOneMarketRepository oneMarketRepository;
    private final TradeAccountRepository accountRepository;
    private final TradePlanRepository planRepository;
    private final TradeStepRepository stepRepository;

    @Autowired
    public BinanceOneMarketService(BinanceService binanceService,
                                   BinanceDepthService depthService,
                                   BinanceExchangeInfoService exchangeInfoService,
                                   TradeOneMarketRepository microRepository,
                                   TradeAccountRepository accountRepository,
                                   TradePlanRepository planRepository,
                                   TradeStepRepository stepRepository) {
        this.binanceService = binanceService;
        this.depthService = depthService;
        this.exchangeInfoService = exchangeInfoService;
        this.oneMarketRepository = microRepository;
        this.accountRepository = accountRepository;
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
    }

    public void startPlan(TradeAccount account, TradeOneMarket oneMarket) {
        startFirstStep(account, oneMarket);
        saveSubplan(oneMarket);
    }

    public void cancelPlan(TradeAccount account, TradePlan plan) {
        TradeOneMarket activePlan = loadSubplan(plan.getId());
        cancel(account, activePlan);
    }

    public void deletePlan(TradePlan plan) {
        oneMarketRepository.deleteAllByPlanId(plan.getId());
        stepRepository.deleteAllByPlanId(plan.getId());
    }

    @Scheduled(fixedDelay = 20000)
    public void checkOrders() {
        List<TradeOneMarket> activePlans = this.oneMarketRepository.findAllByStatus(TradePlanStatus.ACTIVE);
        for(TradeOneMarket activePlan: activePlans) {
            addStepsToMarket(activePlan);
            TradeAccount account = accountRepository.findOne(activePlan.getAccountId());
            //check first step first then step back
            TradeStep firstStep = activePlan.getActiveFirstStep();
            if(firstStep != null) {
                checkStep(account, activePlan, firstStep);
            }
            TradeStep stepBack = activePlan.getActiveStepBack();
            if(stepBack != null) {
                checkStep(account, activePlan, stepBack);
            }
        }
    }

    private void checkStep(TradeAccount account, TradeOneMarket oneMarket, TradeStep step) {
        try {
            BinanceOrderResult orderResult = binanceService.getOrderStatus(account, step.getSymbol(), step.getOrderId());
            if("CANCELED".equals(orderResult.getStatus())) {
                log.info("Order " + orderResult.getOrderId() + " was cancelled. Cancel plan #" + oneMarket.getPlanId());
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
        } catch (BadRequestException e) {
            log.info("Order " + step.getOrderId() + " from one-market plan #" + oneMarket.getPlanId() + " failed to check status");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void cancel(TradeAccount account, TradeOneMarket oneMarket) {
        TradeStep firstStep = oneMarket.getActiveFirstStep();
        if(firstStep != null) {
            binanceService.cancelOrder(account, firstStep.getSymbol(), firstStep.getOrderId());
            firstStep.setStatus(TradeStatus.CANCELLED);
            firstStep.setFinishDate(ZonedDateTime.now());
            firstStep.setDirty();
        }
        TradeStep stepBack = oneMarket.getActiveStepBack();
        if(stepBack != null) {
            binanceService.cancelOrder(account, stepBack.getSymbol(), stepBack.getOrderId());
            stepBack.setStatus(TradeStatus.CANCELLED);
            stepBack.setFinishDate(ZonedDateTime.now());
            stepBack.setDirty();
        }
        oneMarket.setStatus(TradePlanStatus.CANCELLED);
        oneMarket.setFinishDate(ZonedDateTime.now());
        saveSubplan(oneMarket);
        planRepository.updateStatus(oneMarket.getPlanId(), TradePlanStatus.CANCELLED.name());
    }

    private void handleFilledOrder(TradeAccount account, TradeOneMarket oneMarket, TradeStep step, BinanceOrderResult orderResult) {
        // update status and calc step in and out filling
        calcStepFillings(step, orderResult);
        step.setFinishDate(ZonedDateTime.now());
        step.setStatus(TradeStatus.DONE);
        step.setDirty();

        if(step.getStep() == 1) {
            log.info("Plan #" + oneMarket.getPlanId() + " firstStep filled. Move traded coins to stepBack.");
            startStepBack(account, oneMarket, step, orderResult);
        } else {
            TradeStep firstStep = oneMarket.getActiveFirstStep();
            log.info("Plan #" + oneMarket.getPlanId() + " stepBack filled. Check if a firstStep still exists.");
            if(firstStep != null) {
                // cancel firstStep; restart stepBack if firstStep got filling in the meantime
                BinanceOrderResult cancelResult = binanceService.cancelStep(account, firstStep);
                calcStepFillings(firstStep, cancelResult);
                double diffAmount = firstStep.getOutAmount() - step.getInAmount();
                if(exchangeInfoService.isTradeBigEnough(step.getSymbol(), step.getOutCurrency(), diffAmount, step.getPrice())) {
                    // means that in the meantime firstStep got some filling
                    step.setInAmount(firstStep.getOutAmount());
                    binanceService.openStepOrder(account, step);
                    saveSubplan(oneMarket);
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

            // restart plan if auto restart is turned on
            if(oneMarket.isAutoRestart()) {
                log.info("Restart plan #" + oneMarket.getPlanId());
                startFirstStep(account, oneMarket);
            } else {
                oneMarket.setStatus(TradePlanStatus.FINISHED);
            }
        }
        saveSubplan(oneMarket);
    }

    private void handlePartFilledOrder(TradeAccount account, TradeOneMarket oneMarket, TradeStep step, BinanceOrderResult orderResult) {
        // is new filling lower than minimum trade amount or filling equals to orderFilling? handle as unfilled
        String symbol = orderResult.getSymbol();
        double executedQty = Double.parseDouble(orderResult.getExecutedQty()) - step.getOrderFilled();
        double price = Double.parseDouble(orderResult.getPrice());
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), executedQty, price)) {
            handleUnfilledOrder(account, oneMarket, step, orderResult);
            return;
        }

        // is rest of filling lower than minimum trade amount? handle as filled and cancel order
        double origQty = Double.parseDouble(orderResult.getOrigQty());
        if(!exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), (origQty - executedQty), price)) {
            if(!"CANCELED".equals(orderResult.getStatus())) {
                //TODO when cancel firstStep check if it got filling in the meantime and react on that
                try {
                    binanceService.cancelOrder(account, step.getSymbol(), step.getOrderId());
                } catch (Exception e) {}
            }
            handleFilledOrder(account, oneMarket, step, orderResult);
            return;
        }

        // step set orderFilling and update step in and out filling
        calcStepFillings(step, orderResult);
        step.setDirty();
        step.addOrderFilled(executedQty);

        if(step.getStep() == 1) {
            log.info("Plan #" + oneMarket.getPlanId() + " firstStep got a part fill. Move traded coins to stepBack.");
            startStepBack(account, oneMarket, step, orderResult);
        } else {
            log.info("Plan #" + oneMarket.getPlanId() + " stepBack got a part fill. Keep going.");
            handleUnfilledOrder(account, oneMarket, step, orderResult);
        }
        saveSubplan(oneMarket);
    }

    private void handleUnfilledOrder(TradeAccount account, TradeOneMarket oneMarket, TradeStep step, BinanceOrderResult orderResult) {
        // adjust price if necessary
        double goodPrice = depthService.getGoodTradePrice(step);
        if(goodPrice != step.getPrice()) {
            log.info("Plan #" + oneMarket.getPlanId() + (step.getStep() == 1 ? " firstStep" : " stepBack") + " price adjusting");
            if(!"CANCELED".equals(orderResult.getStatus())) {
                BinanceOrderResult cancelResult = binanceService.cancelStep(account, step);
                // check if there was a filling in meantime
                String symbol = cancelResult.getSymbol();
                double executedQty = Double.parseDouble(cancelResult.getExecutedQty()) - step.getOrderFilled();
                double price = Double.parseDouble(cancelResult.getPrice());
                if(exchangeInfoService.isTradeBigEnough(symbol, TradeUtil.getAltCoin(symbol), executedQty, price)) {
                    // there was a meaningful filling
                    log.info("Plan #" + oneMarket.getPlanId() + " cancelled step had filling in the meantime. Handle that now.");
                    handlePartFilledOrder(account, oneMarket, step, cancelResult);
                    return;
                }
            }
            // start the step again with new price
            step.setPrice(goodPrice);
            binanceService.openStepOrder(account, step);
            saveSubplan(oneMarket);
        }
    }

    private void startFirstStep(TradeAccount account, TradeOneMarket oneMarket) {
        TradeStep firstStep = createFirstStep(oneMarket);
        oneMarket.addStep(firstStep);
        binanceService.openStepOrder(account, firstStep);
    }

    private void startStepBack(TradeAccount account, TradeOneMarket oneMarket, TradeStep firstStep, BinanceOrderResult orderResult) {
        TradeStep stepBack = oneMarket.getActiveStepBack();
        if(stepBack != null) {
            stepBack.setDirty();
            log.info("Plan #" + oneMarket.getPlanId() + " add traded coins to existing stepBack.");
            // cancel stepBack then add traded coins to it, recalc priceThreshold and restart it
            BinanceOrderResult cancelResult = binanceService.cancelStep(account, stepBack);
            calcStepFillings(stepBack, cancelResult);
            double newAmount = firstStep.getOutAmount() - stepBack.getInAmount();
            double newThreshold = calcPriceThreshold(orderResult, oneMarket.getMinProfit());
            double oldAmount = stepBack.getInAmount() - stepBack.getInFilled();
            double oldThreshold = stepBack.getPriceThreshold();

            double avgThreshold = avgPriceThreshold(orderResult.getSymbol(), newAmount, newThreshold, oldAmount, oldThreshold);
            stepBack.setInAmount(firstStep.getOutAmount());
            stepBack.setPriceThreshold(avgThreshold);

            stepBack.setPrice(0d);
            stepBack.setPrice(depthService.getGoodTradePrice(stepBack));
        } else {
            log.info("Plan #" + oneMarket.getPlanId() + " create new stepBack.");
            stepBack = createStepBack(oneMarket, firstStep, orderResult);
            oneMarket.addStep(stepBack);
        }
        binanceService.openStepOrder(account, stepBack);
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
        step.setPrice(depthService.getGoodTradePrice(step));
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
        step.setPrice(depthService.getGoodTradePrice(step));
        step.setInCurrency(firstStep.getOutCurrency());
        step.setInAmount(firstStep.getOutAmount());
        step.setOutCurrency(TradeUtil.otherCur(step.getSymbol(), step.getInCurrency()));
        return step;
    }

    private void calcStepFillings(TradeStep step, BinanceOrderResult orderResult) {
        double executedAltCoin = Double.parseDouble(orderResult.getExecutedQty()) - step.getOrderFilled();
        double executedBaseCoin = executedAltCoin * Double.parseDouble(orderResult.getPrice());
        if(TradeUtil.isBuy(step.getSide())) {
            step.addInFilled(executedBaseCoin);
            step.addOutAmount(executedAltCoin);
        } else {
            step.addInFilled(executedAltCoin);
            step.addOutAmount(executedBaseCoin);
        }
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
                }
            }
        }
    }
}
