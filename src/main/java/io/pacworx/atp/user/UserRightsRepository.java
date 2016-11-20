package io.pacworx.atp.user;

import io.pacworx.atp.user.UserRights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRightsRepository extends JpaRepository<UserRights, Long> {
}
