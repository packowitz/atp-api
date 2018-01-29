package io.pacworx.atp.autotrade;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeUserRepository extends CrudRepository<TradeUser, Long> {

    TradeUser findByUsername(String username);
}
