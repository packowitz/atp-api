package io.pacworx.atp.autotrade.domain;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Repository
public interface TradeScheduleLockRepository extends CrudRepository<TradeScheduleLock, String> {

    @Modifying
    @Transactional
    @Query(value="UPDATE trade_schedule_lock SET locked = true, startedTimestamp = :now where id = :id and locked = false")
    int lock(@Param("id")String name, @Param("now")ZonedDateTime now);

    default boolean lock(String name) {
        return this.lock(name, ZonedDateTime.now()) == 1;
    }

    @Modifying
    @Transactional
    @Query(value="UPDATE trade_schedule_lock SET locked = false, finishedTimestamp = :now where id = :id and locked = true")
    int unlock(@Param("id")String name, @Param("now")ZonedDateTime now);

    default boolean unlock(String name) {
        return this.unlock(name, ZonedDateTime.now()) == 1;
    }
}
