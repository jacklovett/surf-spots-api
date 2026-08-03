package com.lovettj.surfspotsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.environmental-alerts.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class EnvironmentalAlertSyncScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentalAlertSyncScheduler.class);

    private final EnvironmentalAlertSyncService environmentalAlertSyncService;

    public EnvironmentalAlertSyncScheduler(EnvironmentalAlertSyncService environmentalAlertSyncService) {
        this.environmentalAlertSyncService = environmentalAlertSyncService;
    }

    @Scheduled(cron = "${app.environmental-alerts.sync-cron:0 */30 * * * *}")
    public void syncEnvironmentalAlerts() {
        try {
            EnvironmentalAlertSyncService.SyncResult result =
                    environmentalAlertSyncService.syncWatchedSpots();
            if (result.alertsCreated() > 0 || result.alertsUpdated() > 0 || result.expiredCount() > 0) {
                logger.info(
                        "Environmental alert sync created={} updated={} expired={}",
                        result.alertsCreated(),
                        result.alertsUpdated(),
                        result.expiredCount());
            }
        } catch (RuntimeException syncException) {
            logger.warn(
                    "Environmental alert sync failed: {}",
                    syncException.getMessage(),
                    syncException);
        }
    }
}
