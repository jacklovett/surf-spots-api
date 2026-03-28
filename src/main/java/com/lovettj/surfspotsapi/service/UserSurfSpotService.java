package com.lovettj.surfspotsapi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.dto.SurfedSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSpotsDTO;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import com.lovettj.surfspotsapi.enums.BeachBottomType;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class UserSurfSpotService {

    private final UserSurfSpotRepository userSurfSpotRepository;
    private final WatchListRepository watchListRepository;
    private final UserRepository userRepository;
    private final SurfSpotRepository surfSpotRepository;

    public UserSurfSpotService(UserSurfSpotRepository userSurfSpotRepository,
                               WatchListRepository watchListRepository,
                               UserRepository userRepository,
                               SurfSpotRepository surfSpotRepository) {
        this.userSurfSpotRepository = userSurfSpotRepository;
        this.watchListRepository = watchListRepository;
        this.userRepository = userRepository;
        this.surfSpotRepository = surfSpotRepository;
    }

    public UserSurfSpotsDTO getUserSurfSpotsSummary(String userId) {
        List<UserSurfSpot> userSurfSpots = userSurfSpotRepository.findByUserId(userId);

        if (userSurfSpots.isEmpty()) {
            return new UserSurfSpotsDTO(0, 0, 0, null, null, null, Collections.emptyList());
        }

        int totalCount = userSurfSpots.size();
        Set<String> distinctCountries = getDistinctCountries(userSurfSpots);
        Set<String> distinctContinents = getDistinctContinents(userSurfSpots);
        int countryCount = distinctCountries.size();
        int continentCount = distinctContinents.size();
        SurfSpotType mostSurfedSpotType = getMostSurfedSpotType(userSurfSpots);
        BeachBottomType mostSurfedBeachType = getMostSurfedBeachType(userSurfSpots);
        SkillLevel skillLevel = getSkillLevel(userSurfSpots);
        List<SurfedSpotDTO> surfedSpots = mapToSurfSpotDTO(userSurfSpots);

        return UserSurfSpotsDTO.builder()
                .totalCount(totalCount)
                .countryCount(countryCount)
                .continentCount(continentCount)
                .mostSurfedSpotType(mostSurfedSpotType)
                .mostSurfedBeachBottomType(mostSurfedBeachType)
                .skillLevel(skillLevel)
                .surfedSpots(surfedSpots).build();
    }

    @Transactional
    public void addUserSurfSpot(String userId, Long spotId) {
        // Check if the spot is already in the user's list
        Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

        if (existingEntry.isEmpty()) {
            // Load the User and SurfSpot entities from the repository to avoid transient entity issues
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            SurfSpot surfSpot = surfSpotRepository.findById(spotId)
                    .orElseThrow(() -> new RuntimeException("Surf spot not found with ID: " + spotId));

            // Create a new entry with managed entities
            UserSurfSpot newEntry = UserSurfSpot.builder()
                    .user(user)
                    .surfSpot(surfSpot)
                    .isFavourite(false)
                    .build();
            userSurfSpotRepository.save(newEntry);
        }
    }

    public boolean isUserSurfedSpot(String userId, Long spotId) {
        Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);
        return existingEntry.isPresent();
    }

    public void removeUserSurfSpot(String userId, Long spotId) {
        Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);
        existingEntry.ifPresent(userSurfSpotRepository::delete);
    }

    public void toggleIsFavourite(String userId, Long spotId) {
        Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

        if (existingEntry.isPresent()) {
            UserSurfSpot userSurfSpot = existingEntry.get();
            userSurfSpot.setFavourite(!userSurfSpot.isFavourite());
            userSurfSpotRepository.save(userSurfSpot);
        }
    }

    private Set<String> getDistinctCountries(List<UserSurfSpot> userSurfSpots) {
        return userSurfSpots.stream()
                .map(UserSurfSpotService::resolveCountryName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Set<String> getDistinctContinents(List<UserSurfSpot> userSurfSpots) {
        return userSurfSpots.stream()
                .map(UserSurfSpotService::resolveContinentName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static String resolveCountryName(UserSurfSpot uss) {
        SurfSpot spot = uss.getSurfSpot();
        if (spot == null || spot.getRegion() == null) {
            return null;
        }
        Country country = spot.getRegion().getCountry();
        return country != null ? country.getName() : null;
    }

    private static String resolveContinentName(UserSurfSpot uss) {
        SurfSpot spot = uss.getSurfSpot();
        if (spot == null || spot.getRegion() == null) {
            return null;
        }
        Country country = spot.getRegion().getCountry();
        if (country == null || country.getContinent() == null) {
            return null;
        }
        return country.getContinent().getName();
    }

    private SurfSpotType getMostSurfedSpotType(List<UserSurfSpot> userSurfSpots) {
        return getMostCommonAttribute(userSurfSpots, uss -> {
            SurfSpot s = uss.getSurfSpot();
            return s != null ? s.getType() : null;
        });
    }

    private BeachBottomType getMostSurfedBeachType(List<UserSurfSpot> userSurfSpots) {
        return getMostCommonAttribute(userSurfSpots, uss -> {
            SurfSpot s = uss.getSurfSpot();
            return s != null ? s.getBeachBottomType() : null;
        });
    }

    private SkillLevel getSkillLevel(List<UserSurfSpot> userSurfSpots) {
        return getMostCommonAttribute(userSurfSpots, uss -> {
            SurfSpot s = uss.getSurfSpot();
            return s != null ? s.getSkillLevel() : null;
        });
    }

    private List<SurfedSpotDTO> mapToSurfSpotDTO(List<UserSurfSpot> userSurfSpots) {
        if (userSurfSpots.isEmpty()) {
            return Collections.emptyList();
        }
        
        String userId = userSurfSpots.get(0).getUser().getId();
        
        // Get all watched spot IDs for this user
        Set<Long> watchedSpotIds = watchListRepository.findByUserId(userId).stream()
                .map(wls -> wls.getSurfSpot().getId())
                .collect(Collectors.toSet());
        
        return userSurfSpots.stream()
                .map(userSurfSpot -> {
                    SurfSpotDTO surfSpotDTO = new SurfSpotDTO(userSurfSpot.getSurfSpot());
                    // Set both flags
                    surfSpotDTO.setIsSurfedSpot(true);
                    surfSpotDTO.setIsWatched(watchedSpotIds.contains(userSurfSpot.getSurfSpot().getId()));
                    return SurfedSpotDTO.fromUserSurfSpot(userSurfSpot, surfSpotDTO);
                })
                .toList();
    }

    /**
     * Most frequent non-null value from mapped attributes. Uses a plain map count so we never pass
     * null keys into {@code Collectors.groupingBy} (novelty spots may have null type/beach/skill).
     */
    private <T> T getMostCommonAttribute(List<UserSurfSpot> userSurfSpots, java.util.function.Function<UserSurfSpot, T> mapper) {
        Map<T, Long> counts = new HashMap<>();
        for (UserSurfSpot uss : userSurfSpots) {
            T attr = mapper.apply(uss);
            if (attr != null) {
                counts.merge(attr, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
