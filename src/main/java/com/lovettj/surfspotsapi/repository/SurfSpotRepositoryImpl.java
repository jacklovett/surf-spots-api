package com.lovettj.surfspotsapi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import com.lovettj.surfspotsapi.dto.SurfSpotFilterDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotBoundsFilterDTO;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;

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
