package io.pacworx.atp.autotrade.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradeOneMarketRepository extends CrudRepository<TradeOneMarket, Long> {

    List<TradeOneMarket> findAllByStatus(TradePlanStatus status);

    TradeOneMarket findByPlanId(long planId);

    TradeOneMarket findByPlanIdAndStatus(long planId, TradePlanStatus status);

    @Transactional
    int deleteAllByPlanId(long planId);
}
