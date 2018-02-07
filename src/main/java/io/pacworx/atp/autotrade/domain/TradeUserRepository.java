package io.pacworx.atp.autotrade.domain;

import io.pacworx.atp.autotrade.domain.TradeUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeUserRepository extends CrudRepository<TradeUser, Long> {

    TradeUser findByUsername(String username);
}
