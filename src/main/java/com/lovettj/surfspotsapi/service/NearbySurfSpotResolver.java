package com.lovettj.surfspotsapi.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.SurfSpotStatus;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.util.CoordinateDistanceUtil;
import com.lovettj.surfspotsapi.util.CoordinateDistanceUtil.CoordinateBoundingBox;

/**
 * Resolves a confident nearest approved surf spot name from GPS coordinates.
 * Uses the same at-spot radius as the frontend live-session flow.
 */
@Service
public class NearbySurfSpotResolver {

    private static final double SPOT_LOOKUP_RADIUS_KM = 1.0;

    private final SurfSpotRepository surfSpotRepository;

    public NearbySurfSpotResolver(SurfSpotRepository surfSpotRepository) {
        this.surfSpotRepository = surfSpotRepository;
    }

    /**
     * Returns an approved spot name only when the nearest catalog spot is within
     * {@link CoordinateDistanceUtil#AT_SPOT_RADIUS_KM} and unambiguous among nearby spots.
     */
    public Optional<String> findApprovedSpotNameNearCoordinates(double latitude, double longitude) {
        CoordinateBoundingBox boundingBox =
                CoordinateDistanceUtil.boundingBoxAroundCoordinates(
                        latitude, longitude, SPOT_LOOKUP_RADIUS_KM);

        SurfSpotBoundsFilterDTO filters = new SurfSpotBoundsFilterDTO();
        filters.setMinLatitude(boundingBox.minLatitude());
        filters.setMaxLatitude(boundingBox.maxLatitude());
        filters.setMinLongitude(boundingBox.minLongitude());
        filters.setMaxLongitude(boundingBox.maxLongitude());
        filters.setStatus(SurfSpotStatus.APPROVED);

        List<SurfSpot> candidates = surfSpotRepository.findWithinBoundsWithFilters(filters);
        List<SpotDistance> withinAtSpotRadius = new ArrayList<>();

        for (SurfSpot spot : candidates) {
            if (spot.getLatitude() == null || spot.getLongitude() == null) {
                continue;
            }
            String spotName = spot.getName() != null ? spot.getName().trim() : "";
            if (spotName.isEmpty()) {
                continue;
            }
            double distanceKm = CoordinateDistanceUtil.distanceKm(
                    latitude, longitude, spot.getLatitude(), spot.getLongitude());
            if (distanceKm <= CoordinateDistanceUtil.AT_SPOT_RADIUS_KM) {
                withinAtSpotRadius.add(new SpotDistance(spotName, distanceKm));
            }
        }

        if (withinAtSpotRadius.isEmpty()) {
            return Optional.empty();
        }

        withinAtSpotRadius.sort(Comparator.comparingDouble(SpotDistance::distanceKm));
        SpotDistance nearest = withinAtSpotRadius.get(0);
        if (withinAtSpotRadius.size() >= 2) {
            SpotDistance secondNearest = withinAtSpotRadius.get(1);
            double gapKm = secondNearest.distanceKm() - nearest.distanceKm();
            if (gapKm < CoordinateDistanceUtil.MIN_CLEAR_SPOT_GAP_KM) {
                return Optional.empty();
            }
        }

        return Optional.of(nearest.spotName());
    }

    private record SpotDistance(String spotName, double distanceKm) {}
}
