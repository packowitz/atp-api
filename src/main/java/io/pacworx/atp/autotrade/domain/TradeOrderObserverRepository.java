package io.pacworx.atp.autotrade.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradeOrderObserverRepository extends CrudRepository<TradeOrderObserver, Long> {

    List<TradeOrderObserver> getAllByPlanType(TradePlanType planType);

    List<TradeOrderObserver> getAllByPlanId(long planId);

    @Transactional
    int deleteAllByPlanId(long planId);
}
