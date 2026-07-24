package com.lovettj.surfspotsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.watch-list.email-alerts-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class WatchListNotificationEmailScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(WatchListNotificationEmailScheduler.class);

    private final WatchListNotificationEmailService watchListNotificationEmailService;

    public WatchListNotificationEmailScheduler(
            WatchListNotificationEmailService watchListNotificationEmailService) {
        this.watchListNotificationEmailService = watchListNotificationEmailService;
    }

    /** Daily check for swell/event watch-list emails. */
    @Scheduled(cron = "${app.watch-list.email-alerts-cron:0 0 8 * * *}")
    public void sendWatchListAlertEmails() {
        try {
            int sentCount = watchListNotificationEmailService.processWatchListAlertEmails();
            if (sentCount > 0) {
                logger.info("Sent {} watch-list alert email(s)", sentCount);
            }
        } catch (RuntimeException processException) {
            logger.warn(
                    "Watch-list alert email job failed: {}",
                    processException.getMessage(),
                    processException);
        }
    }
}
