package com.lovettj.surfspotsapi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveDirection;

@Repository
public class SurfSpotRepositoryImpl implements SurfSpotRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

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
        // Type (enum)
        if (filters.getType() != null && !filters.getType().isEmpty()) {
            predicates.add(root.get("type").in(filters.getType()));
        }

        // SkillLevel (enum) - smart filtering: if filtering by "Intermediate", also match "Beginner - Intermediate", "Intermediate - Advanced", and "All Levels"
        if (filters.getSkillLevel() != null && !filters.getSkillLevel().isEmpty()) {
            List<SkillLevel> expandedSkillLevels = expandEnumFilter(filters.getSkillLevel(), SkillLevel.values());
            // Also include "All Levels" as it matches any filter
            if (!expandedSkillLevels.contains(SkillLevel.ALL_LEVELS)) {
                expandedSkillLevels.add(SkillLevel.ALL_LEVELS);
            }
            predicates.add(root.get("skillLevel").in(expandedSkillLevels));
        }

        // BeachBottomType (enum)
        if (filters.getBeachBottomType() != null && !filters.getBeachBottomType().isEmpty()) {
            predicates.add(root.get("beachBottomType").in(filters.getBeachBottomType()));
        }

        // Tide (enum) - smart filtering: if filtering by "Low", also match "Low - Mid" and "Any"
        if (filters.getTide() != null && !filters.getTide().isEmpty()) {
            List<Tide> expandedTides = expandEnumFilter(filters.getTide(), Tide.values());
            // Also include "Any" as it matches any filter
            if (!expandedTides.contains(Tide.ANY)) {
                expandedTides.add(Tide.ANY);
            }
            predicates.add(root.get("tide").in(expandedTides));
        }

        // WaveDirection (enum) - smart filtering: if filtering by "Left", also match "Left and Right"
        if (filters.getWaveDirection() != null && !filters.getWaveDirection().isEmpty()) {
            List<WaveDirection> expandedWaveDirections = expandEnumFilter(filters.getWaveDirection(), WaveDirection.values());
            predicates.add(root.get("waveDirection").in(expandedWaveDirections));
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

        if (filters.getIsWavepool() != null) {
            predicates.add(cb.equal(root.get("isWavepool"), filters.getIsWavepool()));
        }

        if (filters.getMinRating() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), filters.getMinRating()));
        }

        if (filters.getMaxRating() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("rating"), filters.getMaxRating()));
        }

        // SwellDirection (string array) - smart filtering: if filtering by "E", also match "E-NE", "E-SE", etc.
        if (filters.getSwellDirection() != null && !filters.getSwellDirection().isEmpty()) {
            List<Predicate> swellPredicates = new ArrayList<>();
            for (String direction : filters.getSwellDirection()) {
                // Only match non-null swellDirection values
                swellPredicates.add(cb.and(
                    cb.isNotNull(root.get("swellDirection")),
                    cb.like(cb.lower(root.get("swellDirection")), 
                        "%" + direction.toLowerCase() + "%")
                ));
            }
            predicates.add(cb.or(swellPredicates.toArray(new Predicate[0])));
        }

        // WindDirection (string array) - smart filtering: if filtering by "E", also match "E-NE", "E-SE", etc.
        if (filters.getWindDirection() != null && !filters.getWindDirection().isEmpty()) {
            List<Predicate> windPredicates = new ArrayList<>();
            for (String direction : filters.getWindDirection()) {
                // Only match non-null windDirection values
                windPredicates.add(cb.and(
                    cb.isNotNull(root.get("windDirection")),
                    cb.like(cb.lower(root.get("windDirection")), 
                        "%" + direction.toLowerCase() + "%")
                ));
            }
            predicates.add(cb.or(windPredicates.toArray(new Predicate[0])));
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

    /**
     * Expands enum filter to include all enum values that contain the filter value as a substring.
     * For example, if filtering by "Intermediate", it will also include "Beginner - Intermediate" and "Intermediate - Advanced".
     * 
     * @param filterValues The selected filter values
     * @param allEnumValues All possible enum values
     * @return List of expanded enum values that match any of the filter values
     */
    private <T extends Enum<T>> List<T> expandEnumFilter(List<T> filterValues, T[] allEnumValues) {
        return filterValues.stream()
            .flatMap(filterValue -> {
                String filterDisplayName = getEnumDisplayName(filterValue);
                return java.util.Arrays.stream(allEnumValues)
                    .filter(enumValue -> {
                        String enumDisplayName = getEnumDisplayName(enumValue);
                        return enumDisplayName.toLowerCase().contains(filterDisplayName.toLowerCase());
                    });
            })
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Gets the display name from an enum value.
     * Uses reflection to call getDisplayName() if available, otherwise uses name().
     */
    private <T extends Enum<T>> String getEnumDisplayName(T enumValue) {
        try {
            java.lang.reflect.Method getDisplayName = enumValue.getClass().getMethod("getDisplayName");
            return (String) getDisplayName.invoke(enumValue);
        } catch (Exception e) {
            return enumValue.name();
        }
    }
}
