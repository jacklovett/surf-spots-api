package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.dto.UserSurfSpotsDTO;
import com.lovettj.surfspotsapi.enums.WaveDirection;
import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.UserSurfSpot;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.repository.UserSurfSpotRepository;
import com.lovettj.surfspotsapi.repository.WatchListRepository;

@ExtendWith(MockitoExtension.class)
class UserSurfSpotServiceTests {

    @Mock
    private UserSurfSpotRepository userSurfSpotRepository;

    @Mock
    private WatchListRepository watchListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SurfSpotRepository surfSpotRepository;

    @InjectMocks
    private UserSurfSpotService userSurfSpotService;

    private String userId;
    private User user;
    private SurfSpot riverSpotWithNullEnums;

    @BeforeEach
    void setUp() {
        userId = "user-1";
        user = new User();
        user.setId(userId);

        Continent continent = new Continent();
        continent.setName("Europe");
        continent.setSlug("europe");
        Country country = new Country();
        country.setName("Portugal");
        country.setSlug("portugal");
        country.setContinent(continent);
        Region region = new Region();
        region.setName("Lisbon");
        region.setSlug("lisbon");
        region.setCountry(country);

        riverSpotWithNullEnums = new SurfSpot();
        riverSpotWithNullEnums.setId(99L);
        riverSpotWithNullEnums.setName("River Wave");
        riverSpotWithNullEnums.setSlug("river-wave");
        riverSpotWithNullEnums.setRegion(region);
        riverSpotWithNullEnums.setType(null);
        riverSpotWithNullEnums.setBeachBottomType(null);
        riverSpotWithNullEnums.setSkillLevel(null);
    }

    @Test
    void getUserSurfSpotsSummaryShouldNotThrowWhenSpotHasNullTypeBeachOrSkill() {
        UserSurfSpot uss = UserSurfSpot.builder()
                .user(user)
                .surfSpot(riverSpotWithNullEnums)
                .isFavourite(false)
                .build();

        when(userSurfSpotRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(uss));
        when(watchListRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());

        UserSurfSpotsDTO summary = userSurfSpotService.getUserSurfSpotsSummary(userId);

        assertNotNull(summary);
        assertEquals(1, summary.getTotalCount());
        assertNull(summary.getMostSurfedSpotType());
        assertNull(summary.getMostSurfedBeachBottomType());
        assertNull(summary.getMostSurfedWaveDirection());
        assertNull(summary.getSkillLevel());
        assertEquals(1, summary.getSurfedSpots().size());
    }

    @Test
    void getUserSurfSpotsSummaryShouldReturnMostCommonWaveDirection() {
        SurfSpot leftSpot = copySpotWithWaveDirection(riverSpotWithNullEnums, 1L, "Left Point", WaveDirection.LEFT);
        SurfSpot rightSpot = copySpotWithWaveDirection(riverSpotWithNullEnums, 2L, "Right Point", WaveDirection.RIGHT);
        SurfSpot anotherLeftSpot = copySpotWithWaveDirection(riverSpotWithNullEnums, 3L, "Another Left", WaveDirection.LEFT);

        List<UserSurfSpot> userSurfSpots = List.of(
                userSurfSpotFor(leftSpot),
                userSurfSpotFor(rightSpot),
                userSurfSpotFor(anotherLeftSpot));

        when(userSurfSpotRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(userSurfSpots);
        when(watchListRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());

        UserSurfSpotsDTO summary = userSurfSpotService.getUserSurfSpotsSummary(userId);

        assertEquals(WaveDirection.LEFT, summary.getMostSurfedWaveDirection());
    }

    @Test
    void getUserSurfSpotsSummaryShouldReturnSurfedSpotsNewestFirst() {
        SurfSpot olderSpot = copySpotWithWaveDirection(riverSpotWithNullEnums, 1L, "Older", WaveDirection.LEFT);
        SurfSpot newerSpot = copySpotWithWaveDirection(riverSpotWithNullEnums, 2L, "Newer", WaveDirection.RIGHT);

        UserSurfSpot olderEntry = userSurfSpotFor(olderSpot);
        olderEntry.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        UserSurfSpot newerEntry = userSurfSpotFor(newerSpot);
        newerEntry.setCreatedAt(LocalDateTime.of(2025, 6, 15, 10, 0));

        when(userSurfSpotRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(newerEntry, olderEntry));
        when(watchListRepository.findByUserId(anyString())).thenReturn(Collections.emptyList());

        UserSurfSpotsDTO summary = userSurfSpotService.getUserSurfSpotsSummary(userId);

        assertEquals(2L, summary.getSurfedSpots().get(0).getSurfSpot().getId());
        assertEquals(1L, summary.getSurfedSpots().get(1).getSurfSpot().getId());
    }

    private UserSurfSpot userSurfSpotFor(SurfSpot surfSpot) {
        return UserSurfSpot.builder()
                .user(user)
                .surfSpot(surfSpot)
                .isFavourite(false)
                .build();
    }

    private static SurfSpot copySpotWithWaveDirection(
            SurfSpot template, Long id, String name, WaveDirection waveDirection) {
        SurfSpot spot = new SurfSpot();
        spot.setId(id);
        spot.setName(name);
        spot.setSlug(name.toLowerCase().replace(' ', '-'));
        spot.setRegion(template.getRegion());
        spot.setWaveDirection(waveDirection);
        return spot;
    }
}
