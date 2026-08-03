package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.lovettj.surfspotsapi.dto.EnvironmentalAlertCandidate;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.EnvironmentalAlert;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertSeverity;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertStatus;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertType;
import com.lovettj.surfspotsapi.integration.environmental.EnvironmentalAlertProvider;
import com.lovettj.surfspotsapi.repository.EnvironmentalAlertRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;

@ExtendWith(MockitoExtension.class)
class EnvironmentalAlertSyncServiceTest {

    @Mock
    private WatchListRepository watchListRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    @Mock
    private EnvironmentalAlertRepository environmentalAlertRepository;

    @Mock
    private EnvironmentalAlertProvider provider;

    @Mock
    private PlatformTransactionManager transactionManager;

    private EnvironmentalAlertSyncService syncService;
    private SurfSpot surfSpot;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        syncService = new EnvironmentalAlertSyncService(
                watchListRepository,
                surfSpotRepository,
                environmentalAlertRepository,
                List.of(provider),
                transactionManager);
        surfSpot = SurfSpot.builder().id(10L).name("Supertubos").build();
    }

    @Test
    void upsertAlertShouldCreateWhenNoActiveMatch() {
        EnvironmentalAlertCandidate candidate = candidate(EnvironmentalAlertSeverity.WARNING, "Sewage pollution alert");
        when(environmentalAlertRepository.findBySurfSpotIdAndTypeAndExternalIdAndStatus(
                        eq(10L),
                        eq(EnvironmentalAlertType.SEWAGE_OVERFLOW),
                        eq("ext-1"),
                        eq(EnvironmentalAlertStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        EnvironmentalAlertSyncService.UpsertOutcome outcome =
                syncService.upsertAlert(surfSpot, candidate);

        assertEquals(EnvironmentalAlertSyncService.UpsertOutcome.CREATED, outcome);
        ArgumentCaptor<EnvironmentalAlert> alertCaptor = ArgumentCaptor.forClass(EnvironmentalAlert.class);
        verify(environmentalAlertRepository).save(alertCaptor.capture());
        assertEquals(EnvironmentalAlertStatus.ACTIVE, alertCaptor.getValue().getStatus());
        assertEquals("ext-1", alertCaptor.getValue().getExternalId());
    }

    @Test
    void upsertAlertShouldUpdateWhenSeverityChanges() {
        EnvironmentalAlert existing = EnvironmentalAlert.builder()
                .id(1L)
                .surfSpot(surfSpot)
                .type(EnvironmentalAlertType.SEWAGE_OVERFLOW)
                .severity(EnvironmentalAlertSeverity.CAUTION)
                .title("Sewage pollution alert")
                .sourceName("Scottish Water")
                .externalId("ext-1")
                .detectedAt(Instant.parse("2026-07-01T00:00:00Z"))
                .status(EnvironmentalAlertStatus.ACTIVE)
                .build();
        when(environmentalAlertRepository.findBySurfSpotIdAndTypeAndExternalIdAndStatus(
                        10L, EnvironmentalAlertType.SEWAGE_OVERFLOW, "ext-1", EnvironmentalAlertStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        EnvironmentalAlertCandidate candidate = candidate(EnvironmentalAlertSeverity.WARNING, "Sewage pollution alert");
        EnvironmentalAlertSyncService.UpsertOutcome outcome =
                syncService.upsertAlert(surfSpot, candidate);

        assertEquals(EnvironmentalAlertSyncService.UpsertOutcome.UPDATED, outcome);
        assertEquals(EnvironmentalAlertSeverity.WARNING, existing.getSeverity());
        verify(environmentalAlertRepository).save(existing);
    }

    @Test
    void upsertAlertShouldSkipBlankExternalId() {
        EnvironmentalAlertCandidate candidate = new EnvironmentalAlertCandidate(
                EnvironmentalAlertType.SEWAGE_OVERFLOW,
                EnvironmentalAlertSeverity.WARNING,
                "Sewage pollution alert",
                "desc",
                "Scottish Water",
                null,
                "  ",
                Instant.now(),
                null);

        EnvironmentalAlertSyncService.UpsertOutcome outcome =
                syncService.upsertAlert(surfSpot, candidate);

        assertEquals(EnvironmentalAlertSyncService.UpsertOutcome.SKIPPED, outcome);
        verify(environmentalAlertRepository, never()).save(any());
    }

    @Test
    void upsertAlertShouldSkipNullSeverity() {
        EnvironmentalAlertCandidate candidate = new EnvironmentalAlertCandidate(
                EnvironmentalAlertType.SEWAGE_OVERFLOW,
                null,
                "Sewage pollution alert",
                "desc",
                "Scottish Water",
                null,
                "ext-1",
                Instant.now(),
                null);

        assertEquals(
                EnvironmentalAlertSyncService.UpsertOutcome.SKIPPED,
                syncService.upsertAlert(surfSpot, candidate));
        verify(environmentalAlertRepository, never()).save(any());
    }

    @Test
    void syncWatchedSpotsShouldResolveCountryFromSubRegionParent() {
        Country unitedKingdom = Country.builder().name("United Kingdom").build();
        unitedKingdom.generateSlug();
        Region england = Region.builder().name("England").country(unitedKingdom).build();
        england.generateSlug();
        Region cornwall = Region.builder().name("Cornwall").country(unitedKingdom).build();
        cornwall.generateSlug();
        SubRegion subRegion = SubRegion.builder().name("Newquay").region(england).build();
        SurfSpot cornwallSpot = SurfSpot.builder()
                .id(22L)
                .name("Fistral")
                .region(cornwall)
                .subRegion(subRegion)
                .build();

        when(watchListRepository.findDistinctWatchedSurfSpotIds()).thenReturn(List.of(22L));
        when(surfSpotRepository.findById(22L)).thenReturn(Optional.of(cornwallSpot));
        when(provider.supports(unitedKingdom)).thenReturn(true);
        when(provider.fetchAlerts(cornwallSpot)).thenReturn(List.of());
        when(environmentalAlertRepository.findExpiredActiveAlerts(eq(EnvironmentalAlertStatus.ACTIVE), any()))
                .thenReturn(List.of());

        EnvironmentalAlertSyncService.SyncResult result = syncService.syncWatchedSpots();

        assertEquals(1, result.spotsProcessed());
        verify(provider).supports(unitedKingdom);
        verify(provider).fetchAlerts(cornwallSpot);
    }

    private static EnvironmentalAlertCandidate candidate(EnvironmentalAlertSeverity severity, String title) {
        return new EnvironmentalAlertCandidate(
                EnvironmentalAlertType.SEWAGE_OVERFLOW,
                severity,
                title,
                "Active storm overflow into a nearby watercourse.",
                "Scottish Water",
                "https://www.scottishwater.co.uk/",
                "ext-1",
                Instant.parse("2026-07-28T10:15:00Z"),
                Instant.parse("2026-07-30T00:00:00Z"));
    }
}
