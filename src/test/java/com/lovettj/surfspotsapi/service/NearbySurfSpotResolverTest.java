package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.util.CoordinateDistanceUtil;

@ExtendWith(MockitoExtension.class)
class NearbySurfSpotResolverTest {

    private static final double SESSION_LATITUDE = 54.4783;
    private static final double SESSION_LONGITUDE = -8.2779;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    private NearbySurfSpotResolver nearbySurfSpotResolver;

    @BeforeEach
    void setUp() {
        nearbySurfSpotResolver = new NearbySurfSpotResolver(surfSpotRepository);
    }

    @Test
    void findApprovedSpotNameNearCoordinatesShouldReturnNearestWhenClearlyWithinAtSpotRadius() {
        when(surfSpotRepository.findWithinBoundsWithFilters(any(SurfSpotBoundsFilterDTO.class)))
                .thenReturn(List.of(
                        buildSpot("Bundoran Peak", SESSION_LATITUDE + 0.001, SESSION_LONGITUDE),
                        buildSpot("Distant Peak", SESSION_LATITUDE + 0.02, SESSION_LONGITUDE)));

        Optional<String> spotName = nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(
                SESSION_LATITUDE, SESSION_LONGITUDE);

        assertEquals(Optional.of("Bundoran Peak"), spotName);
    }

    @Test
    void findApprovedSpotNameNearCoordinatesShouldReturnEmptyWhenNearestIsOutsideAtSpotRadius() {
        when(surfSpotRepository.findWithinBoundsWithFilters(any(SurfSpotBoundsFilterDTO.class)))
                .thenReturn(List.of(
                        buildSpot(
                                "Far Peak",
                                SESSION_LATITUDE + CoordinateDistanceUtil.AT_SPOT_RADIUS_KM / 111.0 + 0.01,
                                SESSION_LONGITUDE)));

        Optional<String> spotName = nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(
                SESSION_LATITUDE, SESSION_LONGITUDE);

        assertTrue(spotName.isEmpty());
    }

    @Test
    void findApprovedSpotNameNearCoordinatesShouldReturnEmptyWhenTwoSpotsAreAmbiguousWithinRadius() {
        when(surfSpotRepository.findWithinBoundsWithFilters(any(SurfSpotBoundsFilterDTO.class)))
                .thenReturn(List.of(
                        buildSpot("Peak A", SESSION_LATITUDE + 0.0005, SESSION_LONGITUDE),
                        buildSpot("Peak B", SESSION_LATITUDE + 0.0008, SESSION_LONGITUDE)));

        Optional<String> spotName = nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(
                SESSION_LATITUDE, SESSION_LONGITUDE);

        assertTrue(spotName.isEmpty());
    }

    @Test
    void findApprovedSpotNameNearCoordinatesShouldQueryApprovedSpotsOnly() {
        when(surfSpotRepository.findWithinBoundsWithFilters(any(SurfSpotBoundsFilterDTO.class)))
                .thenAnswer(invocation -> {
                    SurfSpotBoundsFilterDTO filters = invocation.getArgument(0);
                    assertEquals(SurfSpotStatus.APPROVED, filters.getStatus());
                    return List.of();
                });

        nearbySurfSpotResolver.findApprovedSpotNameNearCoordinates(SESSION_LATITUDE, SESSION_LONGITUDE);
    }

    private static SurfSpot buildSpot(String name, double latitude, double longitude) {
        SurfSpot spot = SurfSpot.builder().name(name).latitude(latitude).longitude(longitude).build();
        spot.setId(1L);
        return spot;
    }
}
