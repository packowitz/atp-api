package io.pacworx.atp.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserIdOrderByLastActionDateDesc(long userId);

    @Modifying
    @Transactional
    @Query(value="UPDATE feedback SET unread_answers = 0 WHERE id = :feedbackId and user_id = :userId", nativeQuery = true)
    void markAsRead(@Param("userId")long userId, @Param("feedbackId") long feedbackId);

    Long countByTypeAndStatus(FeedbackType type, FeedbackStatus status);

    List<Feedback> findByTypeAndStatusOrderByLastActionDateDesc(FeedbackType type, FeedbackStatus status);
}
