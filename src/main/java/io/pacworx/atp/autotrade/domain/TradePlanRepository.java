package io.pacworx.atp.autotrade.domain;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradePlanRepository extends CrudRepository<TradePlan, Long> {

    @Modifying
    @Transactional
    @Query(value="UPDATE trade_plan SET status = :status WHERE id = :id", nativeQuery = true)
    int updateStatus(@Param("id")long id, @Param("status")String status);

    List<TradePlan> findAllByAccountIdOrderByIdDesc(long accountId);

    List<TradePlan> findAllByStatus(TradePlanStatus status);
}
