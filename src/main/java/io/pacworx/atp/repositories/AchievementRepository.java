package io.pacworx.atp.repositories;

import io.pacworx.atp.domain.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findByUserId(long userId);
}
