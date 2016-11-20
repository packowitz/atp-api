package io.pacworx.atp.feedback;

import io.pacworx.atp.feedback.FeedbackAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FeedbackAnswerRepository extends JpaRepository<FeedbackAnswer, Long> {

    List<FeedbackAnswer> findByUserIdAndFeedbackIdOrderBySendDateAsc(long userId, long feedbackId);

    @Modifying
    @Transactional
    @Query(value="UPDATE feedback_answer SET read_answer = true WHERE feedback_id = :feedbackId and user_id = :userId", nativeQuery = true)
    void markAsRead(@Param("userId")long userId, @Param("feedbackId") long feedbackId);
}
