package com.lovettj.surfspotsapi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;

@Repository
public class SurfSpotRepositoryImpl implements SurfSpotRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    // Map month names to numeric indices (1-12)
    private static final Map<String, Integer> MONTH_INDICES = new HashMap<>();
    static {
        MONTH_INDICES.put("january", 1);
        MONTH_INDICES.put("february", 2);
        MONTH_INDICES.put("march", 3);
        MONTH_INDICES.put("april", 4);
        MONTH_INDICES.put("may", 5);
        MONTH_INDICES.put("june", 6);
        MONTH_INDICES.put("july", 7);
        MONTH_INDICES.put("august", 8);
        MONTH_INDICES.put("september", 9);
        MONTH_INDICES.put("october", 10);
        MONTH_INDICES.put("november", 11);
        MONTH_INDICES.put("december", 12);
    }

    /**
     * Converts a month name to its numeric index (1-12)
     */
    private Integer getMonthIndex(String monthName) {
        if (monthName == null) return null;
        return MONTH_INDICES.get(monthName.toLowerCase());
    }

    /**
     * Checks if a selected month falls within a season range
     * Handles both normal ranges (e.g., March-June) and wrapping ranges (e.g., December-April)
     */
    private boolean isMonthInSeasonRange(String selectedMonth, String seasonStart, String seasonEnd) {
        Integer selectedIndex = getMonthIndex(selectedMonth);
        Integer startIndex = getMonthIndex(seasonStart);
        Integer endIndex = getMonthIndex(seasonEnd);

        if (selectedIndex == null || startIndex == null || endIndex == null) {
            return false;
        }

        // Case 1: Normal range (start <= end), e.g., March (3) - June (6)
        if (startIndex <= endIndex) {
            return selectedIndex >= startIndex && selectedIndex <= endIndex;
        }
        // Case 2: Wrapping range (start > end), e.g., December (12) - April (4)
        // Selected month must be >= start OR <= end
        else {
            return selectedIndex >= startIndex || selectedIndex <= endIndex;
        }
    }

    @Override
    public List<SurfSpot> findByRegionWithFilters(Region region, SurfSpotFilterDTO filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SurfSpot> cq = cb.createQuery(SurfSpot.class);
        Root<SurfSpot> root = cq.from(SurfSpot.class);
        
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("region"), region));
        // Only include surf spots that don't belong to a sub-region
        predicates.add(cb.isNull(root.get("subRegion")));
        addCommonPredicates(cb, root, predicates, filters);
        addPrivateSpotsFilters(cb, root, predicates, filters.getUserId());

        cq.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<SurfSpot> findBySubRegionWithFilters(SubRegion subRegion, SurfSpotFilterDTO filters) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SurfSpot> cq = cb.createQuery(SurfSpot.class);
        Root<SurfSpot> root = cq.from(SurfSpot.class);
        
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("subRegion"), subRegion));
        addCommonPredicates(cb, root, predicates, filters);
        addPrivateSpotsFilters(cb, root, predicates, filters.getUserId());

        cq.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public List<SurfSpot> findWithinBoundsWithFilters(SurfSpotBoundsFilterDTO filters) {
        Double minLat = filters.getMinLatitude();
        Double maxLat = filters.getMaxLatitude();
        Double minLong = filters.getMinLongitude();
        Double maxLong = filters.getMaxLongitude();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SurfSpot> cq = cb.createQuery(SurfSpot.class);
        Root<SurfSpot> root = cq.from(SurfSpot.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.between(root.get("latitude"), minLat, maxLat));
        predicates.add(cb.between(root.get("longitude"), minLong, maxLong));
        
        addCommonPredicates(cb, root, predicates, filters);
        addPrivateSpotsFilters(cb, root, predicates, filters.getUserId());    
        
        cq.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    public SurfSpot findBySlug(@Param("slug") String slug, @Param("userId") String userId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SurfSpot> cq = cb.createQuery(SurfSpot.class);
        Root<SurfSpot> root = cq.from(SurfSpot.class);

        List<Predicate> predicates = new ArrayList<>();
        // Add the slug filter - this was missing!
        predicates.add(cb.equal(root.get("slug"), slug));
        addPrivateSpotsFilters(cb, root, predicates, userId);

        cq.where(predicates.toArray(new Predicate[0]));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private void addCommonPredicates(CriteriaBuilder cb, Root<SurfSpot> root, List<Predicate> predicates, SurfSpotFilterDTO filters) {

        System.out.println("Filters: " + filters);
        // Type (enum)
        if (filters.getType() != null && !filters.getType().isEmpty()) {
            predicates.add(root.get("type").in(filters.getType()));
        }

        // SkillLevel (enum)
        if (filters.getSkillLevel() != null && !filters.getSkillLevel().isEmpty()) {
            predicates.add(root.get("skillLevel").in(filters.getSkillLevel()));
        }

        // BeachBottomType (enum)
        if (filters.getBeachBottomType() != null && !filters.getBeachBottomType().isEmpty()) {
            predicates.add(root.get("beachBottomType").in(filters.getBeachBottomType()));
        }

        // Tide (enum)
        if (filters.getTide() != null && !filters.getTide().isEmpty()) {
            predicates.add(root.get("tide").in(filters.getTide()));
        }

        // WaveDirection (enum)
        if (filters.getWaveDirection() != null && !filters.getWaveDirection().isEmpty()) {
            predicates.add(root.get("waveDirection").in(filters.getWaveDirection()));
        }

        // Parking (enum)
        if (filters.getParking() != null && !filters.getParking().isEmpty()) {
            predicates.add(root.get("parking").in(filters.getParking()));
        }

        if (filters.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), filters.getStatus()));
        }

        if (filters.getBoatRequired() != null) {
            predicates.add(cb.equal(root.get("boatRequired"), filters.getBoatRequired()));
        }

        if (filters.getMinRating() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filters.getMinRating()));
        }

        if (filters.getMaxRating() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("rating"), filters.getMaxRating()));
        }

        if (filters.getSwellDirection() != null) {
            predicates.add(cb.equal(cb.lower(root.get("swellDirection")), filters.getSwellDirection().toLowerCase()));
        }

        if (filters.getWindDirection() != null) {
            predicates.add(cb.equal(cb.lower(root.get("windDirection")), filters.getWindDirection().toLowerCase()));
        }

        if (filters.getSeasonStart() != null) {
            predicates.add(cb.equal(cb.lower(root.get("seasonStart")), filters.getSeasonStart().toLowerCase()));
        }

        if (filters.getSeasonEnd() != null) {
            predicates.add(cb.equal(cb.lower(root.get("seasonEnd")), filters.getSeasonEnd().toLowerCase()));
        }
        // For arrays of enums (hazards, facilities, foodOptions, accommodationOptions), use join or member of if mapped as @ElementCollection or @ManyToMany
        // Example for hazards:
        if (filters.getHazards() != null && !filters.getHazards().isEmpty()) {
            SetJoin<SurfSpot, Object> hazardsJoin = root.joinSet("hazards", JoinType.LEFT);
            predicates.add(hazardsJoin.in(filters.getHazards()));
        }

        if (filters.getFacilities() != null && !filters.getFacilities().isEmpty()) {
            SetJoin<SurfSpot, Object> facilitiesJoin = root.joinSet("facilities", JoinType.LEFT);
            predicates.add(facilitiesJoin.in(filters.getFacilities()));
        }

        if (filters.getFoodOptions() != null && !filters.getFoodOptions().isEmpty()) {
            SetJoin<SurfSpot, Object> foodOptionsJoin = root.joinSet("foodOptions", JoinType.LEFT);
            predicates.add(foodOptionsJoin.in(filters.getFoodOptions()));
        }

        if (filters.getAccommodationOptions() != null && !filters.getAccommodationOptions().isEmpty()) {
            SetJoin<SurfSpot, Object> accommodationOptionsJoin = root.joinSet("accommodationOptions", JoinType.LEFT);
            predicates.add(accommodationOptionsJoin.in(filters.getAccommodationOptions()));
        }

        // Season filtering - check if any selected month falls within the spot's season range
        // Note: This is done in-memory after fetching because JPA Criteria API doesn't easily support
        // complex month range comparisons with wrapping. We'll filter in the service layer instead.
        // For now, we'll use a simpler approach that works for most cases.
        if (filters.getSeasons() != null && !filters.getSeasons().isEmpty()) {
            // We'll need to filter this in the service layer after fetching results
            // because month range comparisons with wrapping are complex in SQL/JPA
            // For now, add a placeholder predicate that will be handled in the service
            // This is a limitation - we'll filter in memory in the service layer
        }
    }

    void addPrivateSpotsFilters(CriteriaBuilder cb, Root<SurfSpot> root, List<Predicate> predicates, String userId) {
        // Status filtering for approved/private
        if (userId != null) {
            predicates.add(cb.or(
                cb.equal(root.get("status"), "APPROVED"),
                cb.and(
                    cb.equal(root.get("status"), "PRIVATE"),
                    cb.equal(root.get("createdBy"), userId)
                )
            ));
        } else {
            predicates.add(cb.equal(root.get("status"), "APPROVED"));
        }
    }
}
