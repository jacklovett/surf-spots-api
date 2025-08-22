package com.lovettj.surfspotsapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.WatchListDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;
import com.lovettj.surfspotsapi.repository.WatchListRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;

@Service
@Transactional
public class WatchListService {
    private final WatchListRepository watchListRepository;
    private final UserRepository userRepository;
    private final SurfSpotRepository surfSpotRepository;

    public WatchListService(WatchListRepository watchListRepository, 
                          UserRepository userRepository,
                          SurfSpotRepository surfSpotRepository) {
        this.watchListRepository = watchListRepository;
        this.userRepository = userRepository;
        this.surfSpotRepository = surfSpotRepository;
    }

    /**
     * Add surf spot to the users watchList
     * 
     * @param userId
     * @param spotId
     */
    public void addSurfSpotToWatchList(String userId, Long spotId) {
        Optional<WatchListSurfSpot> existingEntry = watchListRepository.findByUserIdAndSurfSpotId(userId, spotId);

        if (existingEntry.isEmpty()) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            SurfSpot surfSpot = surfSpotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Surf spot not found"));

            WatchListSurfSpot newEntry = WatchListSurfSpot.builder()
                .user(user)
                .surfSpot(surfSpot)
                .build();

            watchListRepository.save(newEntry);
        }
    }

    public boolean isWatched(String userId, Long spotId) {
        return watchListRepository.findByUserIdAndSurfSpotId(userId, spotId).isPresent();
    }

    /**
     * Find and remove the surf spot from the user's watchList
     * 
     * @param userId
     * @param spotId
     */
    public void removeSurfSpotFromWishList(String userId, Long spotId) {
        Optional<WatchListSurfSpot> existingEntry = watchListRepository.findByUserIdAndSurfSpotId(userId, spotId);
        existingEntry.ifPresent(watchListRepository::delete);
    }

    /**
     * Get users surf spot watchList
     * 
     * @param userId
     * @return WatchListDTO containing surfSpots and notifications
     */
    public WatchListDTO getUsersWatchList(String userId) {
        List<WatchListSurfSpot> watchList = watchListRepository.findByUserId(userId);

        if (watchList.isEmpty()) {
            return new WatchListDTO(Collections.emptyList(), Collections.emptyList());
        }

        List<SurfSpotDTO> surfSpots = mapToSurfSpotDTO(watchList);

        // TODO: Based on watchList, generate real time notifications data

        return WatchListDTO.builder()
                .notifications(Collections.emptyList())
                .surfSpots(surfSpots)
                .build();
    }

    private List<SurfSpotDTO> mapToSurfSpotDTO(List<WatchListSurfSpot> watchListSurfSpots) {
        return watchListSurfSpots.stream()
            .map(wls -> new SurfSpotDTO(wls.getSurfSpot()))
            .toList();
    }
}
