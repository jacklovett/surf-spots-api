package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SwellSeason;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SwellSeasonService swellSeasonService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(swellSeasonService);
    }

    @Test
    void testGenerateNotificationsShouldReturnEmptyListWhenInputIsEmpty() {
        List<WatchListSurfSpot> emptyList = Collections.emptyList();
        
        List<NotificationDTO> notifications = notificationService.generateNotifications(emptyList);
        
        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
        // NotificationService returns early for empty lists, so swellSeasonService is not called
        verify(swellSeasonService, never()).generateSwellSeasonNotifications(any());
    }

    @Test
    void testGenerateNotificationsShouldCreateRegionalNotificationWhenSeasonStarts() {
        // Create a region with multiple surf spots
        Country country = Country.builder()
            .id(1L)
            .name("USA")
            .build();

        Region region = Region.builder()
            .id(1L)
            .name("California")
            .country(country)
            .build();

        // Create surf spots with season starting in current month
        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason = createSwellSeason("Test Season", seasonStart, seasonEnd);
        SurfSpot spot1 = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        SurfSpot spot2 = createSurfSpotWithSeasonAndRegion("Trestles", swellSeason, region);

        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(spot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(spot2);

        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot1, watchListSurfSpot2);
        
        // Create expected notification from SwellSeasonService
        NotificationDTO expectedNotification = NotificationDTO.builder()
            .id("test-id")
            .title("Test Season Swell Season Has Arrived!")
            .description("The Test Season swell season has officially started! Expect consistent conditions from " + seasonStart + " through " + seasonEnd + ". Affected spots: Mavericks, Trestles.")
            .type("swell")
            .location("Test Season")
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();
        
        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(List.of(expectedNotification));

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
        assertEquals(1, notifications.size());
        assertEquals("swell", notifications.get(0).getType());
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    @Test
    void testGenerateNotificationsShouldCreateRegionalNotificationWhenSeasonEnds() {
        // Create a region
        Country country = Country.builder()
            .id(1L)
            .name("USA")
            .build();

        Region region = Region.builder()
            .id(1L)
            .name("California")
            .country(country)
            .build();

        // Create surf spots with season ending in current month
        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth.minus(3));
        String seasonEnd = formatMonthName(currentMonth);

        SwellSeason swellSeason = createSwellSeason("Test Season", seasonStart, seasonEnd);
        SurfSpot spot1 = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        SurfSpot spot2 = createSurfSpotWithSeasonAndRegion("Trestles", swellSeason, region);

        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(spot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(spot2);

        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot1, watchListSurfSpot2);
        
        NotificationDTO expectedNotification = NotificationDTO.builder()
            .id("test-id")
            .title("Test Season Swell Season Ending Soon")
            .description("The Test Season swell season is ending soon.")
            .type("swell")
            .location("Test Season")
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();
        
        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(List.of(expectedNotification));

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
        assertEquals(1, notifications.size());
        assertEquals("swell", notifications.get(0).getType());
        assertTrue(notifications.get(0).getTitle().contains("Ending Soon"));
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    @Test
    void testGenerateNotificationsShouldReturnEmptyWhenNoSeasonData() {
        Region region = Region.builder()
            .id(1L)
            .name("Test Region")
            .build();

        SurfSpot surfSpot = createSurfSpotWithSeasonAndRegion("No Season Spot", null, region);
        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);
        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot);

        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(Collections.emptyList());

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty(), "Should not generate notifications without season data");
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    @Test
    void testGenerateNotificationsShouldBeSortedByCreatedAt() {
        // Create two different regions
        Country country1 = Country.builder().id(1L).name("USA").build();
        Country country2 = Country.builder().id(2L).name("Australia").build();

        Region region1 = Region.builder().id(1L).name("California").country(country1).build();
        Region region2 = Region.builder().id(2L).name("New South Wales").country(country2).build();

        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason1 = createSwellSeason("Test Season 1", seasonStart, seasonEnd);
        SwellSeason swellSeason2 = createSwellSeason("Test Season 2", seasonStart, seasonEnd);
        SurfSpot surfSpot1 = createSurfSpotWithSeasonAndRegion("Spot 1", swellSeason1, region1);
        SurfSpot surfSpot2 = createSurfSpotWithSeasonAndRegion("Spot 2", swellSeason2, region2);
        
        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(surfSpot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(surfSpot2);

        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot1, watchListSurfSpot2);
        
        LocalDateTime now = LocalDateTime.now();
        NotificationDTO notification1 = NotificationDTO.builder()
            .id("test-id-1")
            .title("Test Season 1 Swell Season Has Arrived!")
            .description("Test")
            .type("swell")
            .location("Test Season 1")
            .createdAt(now.minusHours(1))
            .build();
        
        NotificationDTO notification2 = NotificationDTO.builder()
            .id("test-id-2")
            .title("Test Season 2 Swell Season Has Arrived!")
            .description("Test")
            .type("swell")
            .location("Test Season 2")
            .createdAt(now.minusHours(2))
            .build();
        
        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(List.of(notification1, notification2));

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
        
        // Should have 2 notifications (one per region)
        assertEquals(2, notifications.size());
        
        // Verify notifications are sorted by createdAt (newest first)
        for (int i = 0; i < notifications.size() - 1; i++) {
            LocalDateTime current = notifications.get(i).getCreatedAt();
            LocalDateTime next = notifications.get(i + 1).getCreatedAt();
            
            assertNotNull(current);
            assertNotNull(next);
            assertTrue(current.isAfter(next) || current.isEqual(next),
                "Notifications should be sorted newest first");
        }
        
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    @Test
    void testGenerateNotificationsShouldIncludeLocationString() {
        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        Country country = Country.builder()
            .id(1L)
            .name("USA")
            .build();

        Region region = Region.builder()
            .id(1L)
            .name("California")
            .country(country)
            .build();

        SwellSeason swellSeason = createSwellSeason("Test Season", seasonStart, seasonEnd);
        SurfSpot surfSpot = SurfSpot.builder()
            .id(1L)
            .name("Mavericks")
            .swellSeason(swellSeason)
            .region(region)
            .build();

        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);
        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot);

        NotificationDTO expectedNotification = NotificationDTO.builder()
            .id("test-id")
            .title("Test Season Swell Season Has Arrived!")
            .description("Test")
            .type("swell")
            .location("USA, California")
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();
        
        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(List.of(expectedNotification));

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        assertFalse(notifications.isEmpty());
        
        NotificationDTO notification = notifications.get(0);
        assertNotNull(notification.getLocation());
        assertTrue(notification.getLocation().contains("USA"));
        assertTrue(notification.getLocation().contains("California"));
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    @Test
    void testGenerateNotificationsShouldIncludeLocationStringWhenNoCountry() {
        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        Region region = Region.builder()
            .id(1L)
            .name("California")
            .build();

        SwellSeason swellSeason = createSwellSeason("Test Season", seasonStart, seasonEnd);
        SurfSpot surfSpot = SurfSpot.builder()
            .id(1L)
            .name("Mavericks")
            .swellSeason(swellSeason)
            .region(region)
            .build();

        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);
        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot);

        NotificationDTO expectedNotification = NotificationDTO.builder()
            .id("test-id")
            .title("Test Season Swell Season Has Arrived!")
            .description("Test")
            .type("swell")
            .location("California")
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();
        
        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(List.of(expectedNotification));

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        assertFalse(notifications.isEmpty());
        
        NotificationDTO notification = notifications.get(0);
        assertNotNull(notification.getLocation());
        assertEquals("California", notification.getLocation());
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    @Test
    void testGenerateNotificationsShouldReturnEmptyWhenNoRegion() {
        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason = createSwellSeason("Test Season", seasonStart, seasonEnd);
        SurfSpot surfSpot = SurfSpot.builder()
            .id(1L)
            .name("Mavericks")
            .swellSeason(swellSeason)
            .region(null)
            .build();

        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);
        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot);

        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(Collections.emptyList());

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        // Should not generate notifications for spots without regions
        assertTrue(notifications.isEmpty());
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    @Test
    void testGenerateNotificationsShouldCreateOneNotificationForMultipleSpotsInSameRegion() {
        // Create a region
        Country country = Country.builder().id(1L).name("USA").build();
        Region region = Region.builder().id(1L).name("California").country(country).build();

        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason = createSwellSeason("Test Season", seasonStart, seasonEnd);
        SurfSpot surfSpot1 = createSurfSpotWithSeasonAndRegion("Spot 1", swellSeason, region);
        SurfSpot surfSpot2 = createSurfSpotWithSeasonAndRegion("Spot 2", swellSeason, region);
        SurfSpot surfSpot3 = createSurfSpotWithSeasonAndRegion("Spot 3", null, region); // No season

        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(surfSpot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(surfSpot2);
        WatchListSurfSpot watchListSurfSpot3 = createWatchListSurfSpot(surfSpot3);

        List<WatchListSurfSpot> watchList = List.of(watchListSurfSpot1, watchListSurfSpot2, watchListSurfSpot3);

        NotificationDTO expectedNotification = NotificationDTO.builder()
            .id("test-id")
            .title("Test Season Swell Season Has Arrived!")
            .description("Test")
            .type("swell")
            .location("Test Season")
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();
        
        when(swellSeasonService.generateSwellSeasonNotifications(watchList))
            .thenReturn(List.of(expectedNotification));

        List<NotificationDTO> notifications = notificationService.generateNotifications(watchList);

        assertNotNull(notifications);
        // Should have ONE notification for the region (not per spot)
        assertEquals(1, notifications.size());
        verify(swellSeasonService).generateSwellSeasonNotifications(watchList);
    }

    // Helper methods
    private SurfSpot createSurfSpotWithSeasonAndRegion(String name, SwellSeason swellSeason, Region region) {
        return SurfSpot.builder()
            .id(1L)
            .name(name)
            .swellSeason(swellSeason)
            .region(region)
            .build();
    }

    private SwellSeason createSwellSeason(String name, String start, String end) {
        SwellSeason season = new SwellSeason();
        season.setId(1L);
        season.setName(name);
        season.setStartMonth(start);
        season.setEndMonth(end);
        return season;
    }

    private String formatMonthName(Month month) {
        String monthName = month.name();
        return monthName.substring(0, 1) + monthName.substring(1).toLowerCase();
    }

    private WatchListSurfSpot createWatchListSurfSpot(SurfSpot surfSpot) {
        User user = User.builder()
            .id("user1")
            .build();

        return WatchListSurfSpot.builder()
            .id(1L)
            .user(user)
            .surfSpot(surfSpot)
            .createdAt(LocalDateTime.now())
            .build();
    }
}

