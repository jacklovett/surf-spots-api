package com.lovettj.surfspotsapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.dto.WatchListDTO;
import com.lovettj.surfspotsapi.dto.WatchListSpotDTO;
import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;
import com.lovettj.surfspotsapi.repository.WatchListRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class WatchListService {
    private final WatchListRepository watchListRepository;
    private final UserRepository userRepository;
    private final SurfSpotRepository surfSpotRepository;
    private final UserSurfSpotRepository userSurfSpotRepository;
    private final NotificationService notificationService;

    public WatchListService(WatchListRepository watchListRepository, 
                          UserRepository userRepository,
                          SurfSpotRepository surfSpotRepository,
                          UserSurfSpotRepository userSurfSpotRepository,
                          NotificationService notificationService) {
        this.watchListRepository = watchListRepository;
        this.userRepository = userRepository;
        this.surfSpotRepository = surfSpotRepository;
        this.userSurfSpotRepository = userSurfSpotRepository;
        this.notificationService = notificationService;
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

        List<WatchListSpotDTO> surfSpots = mapToSurfSpotDTO(watchList);
        
        // Generate notifications based on watched surf spots
        List<NotificationDTO> notifications = 
            notificationService.generateNotifications(watchList);

        return WatchListDTO.builder()
                .notifications(notifications)
                .surfSpots(surfSpots)
                .build();
    }

    private List<WatchListSpotDTO> mapToSurfSpotDTO(List<WatchListSurfSpot> watchListSurfSpots) {
        if (watchListSurfSpots.isEmpty()) {
            return Collections.emptyList();
        }
        
        String userId = watchListSurfSpots.get(0).getUser().getId();
        
        // Get all surfed spot IDs for this user
        Set<Long> surfedSpotIds = userSurfSpotRepository.findByUserId(userId).stream()
                .map(uss -> uss.getSurfSpot().getId())
                .collect(Collectors.toSet());
        
        return watchListSurfSpots.stream()
            .map(watchListSurfSpot -> {
                SurfSpotDTO surfSpotDTO = new SurfSpotDTO(watchListSurfSpot.getSurfSpot());
                // Set both flags
                surfSpotDTO.setIsWatched(true);
                surfSpotDTO.setIsSurfedSpot(surfedSpotIds.contains(watchListSurfSpot.getSurfSpot().getId()));
                return WatchListSpotDTO.fromWatchListSurfSpot(watchListSurfSpot, surfSpotDTO);
            })
            .toList();
    }
}
