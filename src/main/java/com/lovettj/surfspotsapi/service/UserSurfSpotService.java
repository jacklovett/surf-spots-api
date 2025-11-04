package com.lovettj.surfspotsapi.service;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfedSpotDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSpotsDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import com.lovettj.surfspotsapi.enums.BeachBottomType;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.SurfSpotType;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class UserSurfSpotService {

    private final UserSurfSpotRepository userSurfSpotRepository;

    public UserSurfSpotService(UserSurfSpotRepository userSurfSpotRepository) {
        this.userSurfSpotRepository = userSurfSpotRepository;
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

    public void addUserSurfSpot(String userId, Long spotId) {
        // Check if the spot is already in the user's list
        Optional<UserSurfSpot> existingEntry = userSurfSpotRepository.findByUserIdAndSurfSpotId(userId, spotId);

        if (existingEntry.isEmpty()) {
            // If not found, create a new entry
            UserSurfSpot newEntry = UserSurfSpot.builder()
                    .user(User.builder().id(userId).build())
                    .surfSpot(SurfSpot.builder().id(spotId).build())
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
                .map(uss -> uss.getSurfSpot().getRegion().getCountry().getName())
                .collect(Collectors.toSet());
    }

    private Set<String> getDistinctContinents(List<UserSurfSpot> userSurfSpots) {
        return getDistinctAttributes(userSurfSpots, uss -> uss.getSurfSpot().getRegion().getCountry().getContinent().getName());
    }

    private SurfSpotType getMostSurfedSpotType(List<UserSurfSpot> userSurfSpots) {
        return getMostCommonAttribute(userSurfSpots, uss -> uss.getSurfSpot().getType());
    }

    private BeachBottomType getMostSurfedBeachType(List<UserSurfSpot> userSurfSpots) {
        return getMostCommonAttribute(userSurfSpots, uss -> uss.getSurfSpot().getBeachBottomType());
    }

    private SkillLevel getSkillLevel(List<UserSurfSpot> userSurfSpots) {
        return getMostCommonAttribute(userSurfSpots, uss -> uss.getSurfSpot().getSkillLevel());
    }

    private List<SurfedSpotDTO> mapToSurfSpotDTO(List<UserSurfSpot> userSurfSpots) {
        return userSurfSpots.stream()
                .map(SurfedSpotDTO::fromUserSurfSpot)
                .toList();
    }

    private <T> Set<T> getDistinctAttributes(List<UserSurfSpot> userSurfSpots, java.util.function.Function<UserSurfSpot, T> mapper) {
        return userSurfSpots.stream()
                .map(mapper)
                .collect(Collectors.toSet());
    }

    private <T> T getMostCommonAttribute(List<UserSurfSpot> userSurfSpots, java.util.function.Function<UserSurfSpot, T> mapper) {
        return userSurfSpots.stream()
                .map(mapper)
                .collect(Collectors.groupingBy(attr -> attr, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
