package io.pacworx.atp.autotrade.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradePathRepository extends CrudRepository<TradePath, Long> {

    List<TradePath> findAllByPlanIdOrderByStartDateDesc(long planId);

    List<TradePath> findAllByPlanIdAndStatus(long planId, TradePlanStatus status);

    List<TradePath> findAllByStatus(TradePlanStatus status);

    @Transactional
    int deleteAllByPlanId(long planId);
}
