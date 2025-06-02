package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.dto.SurfSpotDTO;
import com.lovettj.surfspotsapi.dto.WatchListDTO;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;

@ExtendWith(MockitoExtension.class)
class WatchListServiceTests {

    @Mock
    private WatchListRepository watchListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    @InjectMocks
    private WatchListService watchListService;

    private User testUser;
    private SurfSpot testSpot;
    private String testUserId;
    private WatchListSurfSpot testWatchListEntry;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-id-123";
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");

        testSpot = new SurfSpot();
        testSpot.setId(1L);
        testSpot.setName("Test Spot");

        testWatchListEntry = WatchListSurfSpot.builder()
            .user(testUser)
            .surfSpot(testSpot)
            .build();
    }

    @Test
    void testAddSurfSpotToWatchList() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(testSpot));
        when(watchListRepository.findByUserIdAndSurfSpotId(testUserId, 1L))
            .thenReturn(Optional.empty());

        watchListService.addSurfSpotToWatchList(testUserId, 1L);

        verify(watchListRepository).save(any(WatchListSurfSpot.class));
    }

    @Test
    void testGetUsersWatchList() {
        when(watchListRepository.findByUserId(testUserId))
            .thenReturn(Arrays.asList(testWatchListEntry));

        WatchListDTO result = watchListService.getUsersWatchList(testUserId);

        assertNotNull(result);
        assertFalse(result.getSurfSpots().isEmpty());
        assertEquals(testSpot.getName(), result.getSurfSpots().get(0).getName());
    }

    @Test
    void testRemoveSurfSpotFromWishList() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(surfSpotRepository.findById(1L)).thenReturn(Optional.of(testSpot));
        when(watchListRepository.findByUserIdAndSurfSpotId(testUserId, 1L))
            .thenReturn(Optional.of(testWatchListEntry));

        watchListService.removeSurfSpotFromWishList(testUserId, 1L);

        verify(watchListRepository).delete(testWatchListEntry);
    }

    @Test
    void testIsWatched() {
        when(watchListRepository.findByUserIdAndSurfSpotId(testUserId, 1L))
            .thenReturn(Optional.of(testWatchListEntry));

        boolean result = watchListService.isWatched(testUserId, 1L);

        assertTrue(result);
    }
} 