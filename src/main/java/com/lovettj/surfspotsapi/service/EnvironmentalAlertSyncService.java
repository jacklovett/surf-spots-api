package com.lovettj.surfspotsapi.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.lovettj.surfspotsapi.dto.EnvironmentalAlertCandidate;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.EnvironmentalAlert;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertStatus;
import com.lovettj.surfspotsapi.integration.environmental.EnvironmentalAlertProvider;
import com.lovettj.surfspotsapi.repository.EnvironmentalAlertRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;

@Service
public class EnvironmentalAlertSyncService {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentalAlertSyncService.class);

    private final WatchListRepository watchListRepository;
    private final SurfSpotRepository surfSpotRepository;
    private final EnvironmentalAlertRepository environmentalAlertRepository;
    private final List<EnvironmentalAlertProvider> providers;
    private final TransactionTemplate transactionTemplate;

    public EnvironmentalAlertSyncService(
            WatchListRepository watchListRepository,
            SurfSpotRepository surfSpotRepository,
            EnvironmentalAlertRepository environmentalAlertRepository,
            List<EnvironmentalAlertProvider> providers,
            PlatformTransactionManager transactionManager) {
        this.watchListRepository = watchListRepository;
        this.surfSpotRepository = surfSpotRepository;
        this.environmentalAlertRepository = environmentalAlertRepository;
        this.providers = providers;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Provider HTTP stays outside DB transactions; each upsert/expire runs in its own short TX.
     */
    public SyncResult syncWatchedSpots() {
        Instant startedAt = Instant.now();
        List<Long> watchedSpotIds = watchListRepository.findDistinctWatchedSurfSpotIds();
        int spotsProcessed = 0;
        int alertsCreated = 0;
        int alertsUpdated = 0;
        int providerFailures = 0;

        for (Long spotId : watchedSpotIds) {
            SurfSpot surfSpot = surfSpotRepository.findById(spotId).orElse(null);
            if (surfSpot == null) {
                continue;
            }
            Country country = resolveCountry(surfSpot);
            if (country == null) {
                continue;
            }
            spotsProcessed++;

            for (EnvironmentalAlertProvider provider : providers) {
                if (!provider.supports(country)) {
                    continue;
                }
                try {
                    List<EnvironmentalAlertCandidate> candidates = provider.fetchAlerts(surfSpot);
                    for (EnvironmentalAlertCandidate candidate : candidates) {
                        UpsertOutcome outcome = transactionTemplate.execute(
                                status -> upsertAlert(surfSpot, candidate));
                        if (outcome == UpsertOutcome.CREATED) {
                            alertsCreated++;
                        } else if (outcome == UpsertOutcome.UPDATED) {
                            alertsUpdated++;
                        }
                    }
                } catch (RuntimeException providerException) {
                    providerFailures++;
                    logger.warn(
                            "Environmental provider {} failed for spotId={}: {}",
                            provider.getProviderKey(),
                            spotId,
                            providerException.getMessage(),
                            providerException);
                }
            }
        }

        Integer expiredCount = transactionTemplate.execute(status -> expireStaleAlerts(Instant.now()));
        int expired = expiredCount != null ? expiredCount : 0;

        logger.info(
                "Environmental alert sync finished spotsProcessed={} created={} updated={} expired={} failures={} durationMs={}",
                spotsProcessed,
                alertsCreated,
                alertsUpdated,
                expired,
                providerFailures,
                Instant.now().toEpochMilli() - startedAt.toEpochMilli());

        return new SyncResult(spotsProcessed, alertsCreated, alertsUpdated, expired, providerFailures);
    }

    UpsertOutcome upsertAlert(SurfSpot surfSpot, EnvironmentalAlertCandidate candidate) {
        if (candidate == null
                || candidate.externalId() == null
                || candidate.externalId().isBlank()
                || candidate.type() == null
                || candidate.severity() == null
                || candidate.title() == null
                || candidate.title().isBlank()
                || candidate.sourceName() == null
                || candidate.sourceName().isBlank()) {
            return UpsertOutcome.SKIPPED;
        }

        return environmentalAlertRepository
                .findBySurfSpotIdAndTypeAndExternalIdAndStatus(
                        surfSpot.getId(), candidate.type(), candidate.externalId(), EnvironmentalAlertStatus.ACTIVE)
                .map(existing -> updateExisting(existing, candidate))
                .orElseGet(() -> createNew(surfSpot, candidate));
    }

    private UpsertOutcome createNew(SurfSpot surfSpot, EnvironmentalAlertCandidate candidate) {
        EnvironmentalAlert alert = EnvironmentalAlert.builder()
                .surfSpot(surfSpot)
                .type(candidate.type())
                .severity(candidate.severity())
                .title(candidate.title())
                .description(candidate.description())
                .sourceName(candidate.sourceName())
                .sourceUrl(candidate.sourceUrl())
                .externalId(candidate.externalId())
                .detectedAt(candidate.detectedAt() != null ? candidate.detectedAt() : Instant.now())
                .expiresAt(candidate.expiresAt())
                .status(EnvironmentalAlertStatus.ACTIVE)
                .build();
        environmentalAlertRepository.save(alert);
        return UpsertOutcome.CREATED;
    }

    private UpsertOutcome updateExisting(EnvironmentalAlert existing, EnvironmentalAlertCandidate candidate) {
        existing.setSeverity(candidate.severity());
        if (candidate.title() != null && !candidate.title().isBlank()) {
            existing.setTitle(candidate.title());
        }
        if (!Objects.equals(existing.getDescription(), candidate.description())) {
            existing.setDescription(candidate.description());
        }
        if (candidate.sourceName() != null && !candidate.sourceName().isBlank()) {
            existing.setSourceName(candidate.sourceName());
        }
        if (candidate.sourceUrl() != null) {
            existing.setSourceUrl(candidate.sourceUrl());
        }
        if (candidate.detectedAt() != null) {
            existing.setDetectedAt(candidate.detectedAt());
        }
        if (candidate.expiresAt() != null) {
            Instant currentExpiry = existing.getExpiresAt();
            if (currentExpiry == null || candidate.expiresAt().isAfter(currentExpiry)) {
                existing.setExpiresAt(candidate.expiresAt());
            }
        }
        environmentalAlertRepository.save(existing);
        return UpsertOutcome.UPDATED;
    }

    private int expireStaleAlerts(Instant now) {
        List<EnvironmentalAlert> expired = environmentalAlertRepository.findExpiredActiveAlerts(EnvironmentalAlertStatus.ACTIVE, now);
        for (EnvironmentalAlert alert : expired) {
            alert.setStatus(EnvironmentalAlertStatus.EXPIRED);
        }
        if (!expired.isEmpty()) {
            environmentalAlertRepository.saveAll(expired);
        }
        return expired.size();
    }

    private static Country resolveCountry(SurfSpot surfSpot) {
        if (surfSpot.getRegion() != null && surfSpot.getRegion().getCountry() != null) {
            return surfSpot.getRegion().getCountry();
        }
        if (surfSpot.getSubRegion() != null && surfSpot.getSubRegion().getRegion() != null) {
            return surfSpot.getSubRegion().getRegion().getCountry();
        }
        return null;
    }

    enum UpsertOutcome {
        CREATED,
        UPDATED,
        SKIPPED
    }

    public record SyncResult(
            int spotsProcessed,
            int alertsCreated,
            int alertsUpdated,
            int expiredCount,
            int providerFailures) {}
}
