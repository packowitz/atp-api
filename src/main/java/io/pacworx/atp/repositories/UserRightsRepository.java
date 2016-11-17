package io.pacworx.atp.repositories;

import io.pacworx.atp.domain.UserRights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRightsRepository extends JpaRepository<UserRights, Long> {
}
