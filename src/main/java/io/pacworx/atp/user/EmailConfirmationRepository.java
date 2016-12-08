package io.pacworx.atp.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, Long> {

    EmailConfirmation findByUserIdAndEmail(long userId, String email);
}
