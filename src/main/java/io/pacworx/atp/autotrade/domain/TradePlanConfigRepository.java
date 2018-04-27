package io.pacworx.atp.autotrade.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TradePlanConfigRepository extends CrudRepository<TradePlanConfig, Long> {

    @Transactional
    int deleteAllByPlanId(long planId);
}
