package io.pacworx.atp.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClosedBetaRepository extends JpaRepository<ClosedBeta, Long> {

    List<ClosedBeta> findByOrderByRegisterDateDesc();
}
