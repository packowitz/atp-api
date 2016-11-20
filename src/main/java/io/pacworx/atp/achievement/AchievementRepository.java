package io.pacworx.atp.achievement;

import io.pacworx.atp.achievement.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findByUserId(long userId);
}
