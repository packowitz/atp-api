package nz.pacworx.atp.jobs;

import nz.pacworx.atp.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ResetWeeklyStats {
    private static final Logger logger = LoggerFactory.getLogger(ResetWeeklyStats.class);

    @Autowired
    private UserRepository userRepository;

    @Scheduled(cron = "0 0 7 * * MON")
    public void runJob() {
        try {
            int answered = userRepository.resetWeeklyAnsweredStats();
            int started = userRepository.resetWeeklyStartedStats();
            logger.info("Reset weekly Stats job successful: answered=" + answered + ", started=" + started);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
