package com.lovettj.surfspotsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.live-session.overdue-notification-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LiveSessionOverdueNotificationScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(LiveSessionOverdueNotificationScheduler.class);

    private final LiveSessionOverdueNotificationService liveSessionOverdueNotificationService;

    public LiveSessionOverdueNotificationScheduler(
            LiveSessionOverdueNotificationService liveSessionOverdueNotificationService) {
        this.liveSessionOverdueNotificationService = liveSessionOverdueNotificationService;
    }

    @Scheduled(fixedDelayString = "${app.live-session.overdue-notification-check-ms:300000}")
    public void checkForOverdueLiveSessions() {
        try {
            int sentCount = liveSessionOverdueNotificationService.processOverdueSessions();
            if (sentCount > 0) {
                logger.info("Sent {} live session overdue notification email(s)", sentCount);
            }
        } catch (RuntimeException processException) {
            logger.warn(
                    "Live session overdue notification check failed: {}",
                    processException.getMessage(),
                    processException);
        }
    }
}
