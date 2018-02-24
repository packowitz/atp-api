package io.pacworx.atp.autotrade.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradeStepRepository extends CrudRepository<TradeStep, Long> {

    List<TradeStep> findAllByPlanIdAndSubplanIdOrderByIdDesc(long planId, long subplanId);

    @Transactional
    int deleteAllByPlanId(long planId);
}
