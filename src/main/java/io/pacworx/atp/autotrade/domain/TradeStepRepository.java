package io.pacworx.atp.autotrade.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradeStepRepository extends CrudRepository<TradeStep, Long> {

    List<TradeStep> findAllByPlanIdOrderByIdDesc(long planId);

    List<TradeStep> findAllByPlanIdAndSubplanIdOrderByIdDesc(long planId, long subplanId);

    @Query(value="SELECT price FROM trade_step WHERE symbol = :symbol and status = 'ACTIVE'")
    List<Double> findActivePrices(@Param("symbol") String symbol);

    @Transactional
    int deleteAllByPlanId(long planId);
}
