package io.pacworx.atp.autotrade;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;

@Component
public class BinanceCircleService {
    private static final Logger log = LogManager.getLogger();

    private final BinanceService binanceService;
    private final TradeCircleRepository tradeCircleRepository;
    private final TradeOrderObserverRepository tradeOrderObserverRepository;
    private final TradeAccountRepository tradeAccountRepository;
    private final TradePlanRepository tradePlanRepository;

    @Autowired
    public BinanceCircleService(BinanceService binanceService,
                                TradeCircleRepository tradeCircleRepository,
                                TradeOrderObserverRepository tradeOrderObserverRepository,
                                TradeAccountRepository tradeAccountRepository,
                                TradePlanRepository tradePlanRepository) {
        this.binanceService = binanceService;
        this.tradeCircleRepository = tradeCircleRepository;
        this.tradeOrderObserverRepository = tradeOrderObserverRepository;
        this.tradeAccountRepository = tradeAccountRepository;
        this.tradePlanRepository = tradePlanRepository;
    }

    public void startCircle(TradeAccount account, TradeCircle circle) {
        TradeCircleStep firstStep = circle.getSteps().get(0);
        firstStep.setInCurrency(circle.getStartCurrency());
        firstStep.setInAmount(circle.getStartAmount());

        BinanceOrderResult result = openStepOrder(account, circle, firstStep);

        circle.setStartDate(ZonedDateTime.now());
        tradeCircleRepository.save(circle);

        TradeOrderObserver observer = new TradeOrderObserver();
        observer.setOrderId(result.getOrderId());
        observer.setSymbol(firstStep.getSymbol());
        observer.setBroker("binance");
        observer.setUserId(account.getUserId());
        observer.setAccountId(account.getId());
        observer.setPlanId(circle.getPlanId());
        observer.setPlanType(TradePlanType.CIRCLE);
        observer.setSubplanId(circle.getId());
        observer.setTreshold(circle.getTreshold());
        observer.setCancelOnTreshold(circle.isCancelOnTreshold());
        observer.setCheckDate(ZonedDateTime.now());
        tradeOrderObserverRepository.save(observer);
    }

    @Scheduled(fixedDelay = 20000)
    public void checkCircles() {
        List<TradeOrderObserver> ordersToCheck = this.tradeOrderObserverRepository.getAllByPlanType(TradePlanType.CIRCLE);
        for(TradeOrderObserver orderToCheck: ordersToCheck) {
            try {
                TradeAccount account = this.tradeAccountRepository.findOne(orderToCheck.getAccountId());
                BinanceOrderResult orderResult = this.binanceService.getOrderStatus(account, orderToCheck.getSymbol(), orderToCheck.getOrderId());
                if ("FILLED".equals(orderResult.getStatus())) {
                    log.info("Order " + orderResult.getOrderId() + " is filled. Setting up next circle step.");
                    startNextStep(account, orderToCheck, orderResult);
                } if("CANCELED".equals(orderResult.getStatus())) {
                    log.info("Order " + orderResult.getOrderId() + " was cancelled. Deleting observer.");
                    this.tradePlanRepository.updateStatus(orderToCheck.getPlanId(), TradePlanStatus.CANCELLED.name());
                    this.tradeOrderObserverRepository.delete(orderToCheck.getId());
                } if("PARTIALLY_FILLED".equals(orderResult.getStatus())) {
                    double percExecuted = 100d * Double.parseDouble(orderResult.getExecutedQty()) / Double.parseDouble(orderResult.getOrigQty());
                    if(percExecuted >= orderToCheck.getTreshold()) {
                        log.info("Order " + orderResult.getOrderId() + " is filled over " + orderToCheck.getTreshold() + "%. Setting up next circle step.");
                        if(orderToCheck.isCancelOnTreshold()) {
                            this.binanceService.cancelOrder(account, orderToCheck.getSymbol(), orderToCheck.getOrderId());
                        }
                        startNextStep(account, orderToCheck, orderResult);
                    } else {
                        log.info("Order " + orderResult.getOrderId() + " is partially filled under " + orderToCheck.getTreshold() + "%.");
                    }
                } else {
                    log.info("Order " + orderResult.getOrderId() + " from circle " + orderToCheck.getSubplanId() + " is in status: " + orderResult.getStatus());
                }
            } catch (Exception e) {
                log.error("Check circle " + orderToCheck.getSubplanId() + " and order " + orderToCheck.getOrderId() + " ended with Exception: " + e.getMessage());
            }
        }
    }

    private void startNextStep(TradeAccount account, TradeOrderObserver orderToCheck, BinanceOrderResult orderResult) {
        TradeCircle circle = this.tradeCircleRepository.findOne(orderToCheck.getSubplanId());
        TradeCircleStep currentStep = circle.getCurrentStep();
        currentStep.setStatus(TradeStatus.DONE);
        currentStep.setFinishDate(ZonedDateTime.now());
        currentStep.setOutCurrency(orderResult.getSymbol().replaceFirst(currentStep.getInCurrency(), ""));
        double outAmount;
        if(orderResult.getSide().equalsIgnoreCase("BUY")) {
            outAmount = Double.parseDouble(orderResult.getExecutedQty());
        } else {
            outAmount = Double.parseDouble(orderResult.getExecutedQty()) * Double.parseDouble(orderResult.getPrice());
        }
        currentStep.setOutAmount(outAmount);

        TradeCircleStep nextStep = circle.getNextStep();
        if(nextStep != null) {
            nextStep.setInCurrency(currentStep.getOutCurrency());
            nextStep.setInAmount(currentStep.getOutAmount());

            BinanceOrderResult result = openStepOrder(account, circle, nextStep);

            orderToCheck.setOrderId(result.getOrderId());
            orderToCheck.setSymbol(nextStep.getSymbol());
            this.tradeOrderObserverRepository.save(orderToCheck);
        } else {
            //Circle Finished
            circle.setStatus(TradePlanStatus.FINISHED);
            circle.setActiveStep(null);
            circle.setActiveOrderId(null);
            circle.setFinishDate(ZonedDateTime.now());
            circle.setFinishAmount(currentStep.getOutAmount());
            this.tradePlanRepository.updateStatus(circle.getPlanId(), TradePlanStatus.FINISHED.name());
        }
        this.tradeCircleRepository.save(circle);
    }

    private BinanceOrderResult openStepOrder(TradeAccount account, TradeCircle circle, TradeCircleStep step) {
        double amount;
        if(step.getSide().equalsIgnoreCase("BUY")) {
            amount = step.getInAmount() / step.getPrice();
        } else {
            amount = step.getInAmount();
        }

        TradeOffer offer = new TradeOffer(step.getSymbol(), step.getSide().toUpperCase(), step.getPrice(), amount);
        BinanceOrderResult result = binanceService.openLimitOrder(account, offer);

        circle.setActiveStep(step.getStep());
        circle.setActiveOrderId(result.getOrderId());
        step.setStartDate(ZonedDateTime.now());
        step.setOrderId(result.getOrderId());
        step.setStatus(TradeStatus.ACTIVE);
        double inAmount;
        if(result.getSide().equalsIgnoreCase("BUY")) {
            inAmount = Double.parseDouble(result.getOrigQty()) * Double.parseDouble(result.getPrice());
        } else {
            inAmount = Double.parseDouble(result.getExecutedQty());
        }
        step.setInAmount(inAmount);

        return result;
    }
}
