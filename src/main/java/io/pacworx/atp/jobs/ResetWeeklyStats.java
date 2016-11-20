package io.pacworx.atp.jobs;

import io.pacworx.atp.user.UserRepository;
import io.pacworx.atp.exception.GlobalExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResetWeeklyStats {
    private static final Logger LOGGER = LogManager.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * MON")
    public void runJob() {
        try {
            int answered = userRepository.resetWeeklyAnsweredStats();
            int started = userRepository.resetWeeklyStartedStats();
            LOGGER.info("Reset weekly Stats job successful: answered=" + answered + ", started=" + started);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
