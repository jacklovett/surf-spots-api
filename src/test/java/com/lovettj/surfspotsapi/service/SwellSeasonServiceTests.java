package com.lovettj.surfspotsapi.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SwellSeason;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;

class SwellSeasonServiceTests {

    private SwellSeasonService swellSeasonService;

    @BeforeEach
    void setUp() {
        swellSeasonService = new SwellSeasonService();
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldReturnEmptyListWhenInputIsEmpty() {
        List<WatchListSurfSpot> emptyList = Collections.emptyList();
        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(emptyList);
        
        assertNotNull(notifications);
        assertTrue(notifications.isEmpty());
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldCreateNotificationWhenSeasonStarts() {
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

        SwellSeason swellSeason = createSwellSeason("North Atlantic", seasonStart, seasonEnd);
        SurfSpot spot1 = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        SurfSpot spot2 = createSurfSpotWithSeasonAndRegion("Trestles", swellSeason, region);

        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(spot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(spot2);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot1, watchListSurfSpot2));

        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
        
        // Should have ONE notification for the region
        List<NotificationDTO> startNotifications = notifications.stream()
            .filter(n -> n.getTitle().contains("Has Arrived"))
            .toList();
        
        assertEquals(1, startNotifications.size(), "Should have exactly one regional notification");
        
        // Verify notification details
        NotificationDTO startNotification = startNotifications.get(0);
        assertEquals("swell", startNotification.getType());
        assertTrue(startNotification.getTitle().contains("North Atlantic"));
        assertTrue(startNotification.getTitle().contains("Has Arrived"));
        assertNotNull(startNotification.getLocation());
        assertNotNull(startNotification.getDescription());
        assertTrue(startNotification.getDescription().contains("Mavericks") || 
                   startNotification.getDescription().contains("Trestles"),
            "Description should mention affected spots");
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldCreateNotificationWhenSeasonEnds() {
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

        // Create surf spots with season ending one month from now (so we're one month before end)
        Month currentMonth = LocalDateTime.now().getMonth();
        Month endMonth = currentMonth.plus(1);
        String seasonStart = formatMonthName(currentMonth.minus(3));
        String seasonEnd = formatMonthName(endMonth);

        SwellSeason swellSeason = createSwellSeason("North Atlantic", seasonStart, seasonEnd);
        SurfSpot spot1 = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        SurfSpot spot2 = createSurfSpotWithSeasonAndRegion("Trestles", swellSeason, region);

        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(spot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(spot2);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot1, watchListSurfSpot2));

        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
        
        // Should have ONE notification for the region
        List<NotificationDTO> endNotifications = notifications.stream()
            .filter(n -> n.getTitle().contains("Ending Soon"))
            .toList();
        
        assertEquals(1, endNotifications.size(), "Should have exactly one regional notification");
        
        NotificationDTO endNotification = endNotifications.get(0);
        assertEquals("swell", endNotification.getType());
        assertTrue(endNotification.getTitle().contains("North Atlantic"));
        assertTrue(endNotification.getTitle().contains("Ending Soon"));
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldReturnEmptyWhenNoSeasonData() {
        Region region = Region.builder()
            .id(1L)
            .name("Test Region")
            .build();

        SurfSpot surfSpot = createSurfSpotWithSeasonAndRegion("No Season Spot", null, region);
        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot));

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty(), "Should not generate notifications without season data");
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldReturnEmptyWhenSeasonNotActive() {
        Country country = Country.builder()
            .id(1L)
            .name("USA")
            .build();

        Region region = Region.builder()
            .id(1L)
            .name("California")
            .country(country)
            .build();

        // Create season that's not starting or ending now
        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth.plus(2)); // 2 months from now
        String seasonEnd = formatMonthName(currentMonth.plus(5)); // 5 months from now

        SwellSeason swellSeason = createSwellSeason("North Atlantic", seasonStart, seasonEnd);
        SurfSpot surfSpot = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot));

        assertNotNull(notifications);
        assertTrue(notifications.isEmpty(), "Should not generate notifications when season is not starting or ending");
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldHandleYearBoundary() {
        // Test season that spans year boundary (e.g., September to April)
        Country country = Country.builder()
            .id(1L)
            .name("USA")
            .build();

        Region region = Region.builder()
            .id(1L)
            .name("California")
            .country(country)
            .build();

        // If current month is September, season should start
        Month currentMonth = LocalDateTime.now().getMonth();
        if (currentMonth == Month.SEPTEMBER) {
            SwellSeason swellSeason = createSwellSeason("North Atlantic", "September", "April");
            SurfSpot surfSpot = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
            WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);

            List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
                List.of(watchListSurfSpot));

            assertFalse(notifications.isEmpty(), "Should generate notification when season starts in September");
        }
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldGroupByRegion() {
        // Create two different regions
        Country country1 = Country.builder().id(1L).name("USA").build();
        Country country2 = Country.builder().id(2L).name("Australia").build();

        Region region1 = Region.builder().id(1L).name("California").country(country1).build();
        Region region2 = Region.builder().id(2L).name("New South Wales").country(country2).build();

        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason1 = createSwellSeason("North Atlantic", seasonStart, seasonEnd);
        SwellSeason swellSeason2 = createSwellSeason("South Pacific", seasonStart, seasonEnd);
        SurfSpot surfSpot1 = createSurfSpotWithSeasonAndRegion("Spot 1", swellSeason1, region1);
        SurfSpot surfSpot2 = createSurfSpotWithSeasonAndRegion("Spot 2", swellSeason2, region2);
        
        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(surfSpot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(surfSpot2);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot1, watchListSurfSpot2));

        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
        
        // Should have 2 notifications (one per region)
        assertEquals(2, notifications.size());
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldGroupBySwellSeason() {
        // Create spots in same region but different swell seasons
        Country country = Country.builder().id(1L).name("USA").build();
        Region region = Region.builder().id(1L).name("California").country(country).build();

        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason1 = createSwellSeasonWithId(1L, "North Atlantic", seasonStart, seasonEnd);
        SwellSeason swellSeason2 = createSwellSeasonWithId(2L, "North Pacific", seasonStart, seasonEnd);
        
        SurfSpot surfSpot1 = createSurfSpotWithSeasonAndRegion("Spot 1", swellSeason1, region);
        SurfSpot surfSpot2 = createSurfSpotWithSeasonAndRegion("Spot 2", swellSeason1, region);
        SurfSpot surfSpot3 = createSurfSpotWithSeasonAndRegion("Spot 3", swellSeason2, region);
        
        WatchListSurfSpot watchListSurfSpot1 = createWatchListSurfSpot(surfSpot1);
        WatchListSurfSpot watchListSurfSpot2 = createWatchListSurfSpot(surfSpot2);
        WatchListSurfSpot watchListSurfSpot3 = createWatchListSurfSpot(surfSpot3);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot1, watchListSurfSpot2, watchListSurfSpot3));

        assertNotNull(notifications);
        // Should have 2 notifications (one per swell season in the region)
        assertEquals(2, notifications.size());
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldUseSwellSeasonNameAsLocation() {
        Country country = Country.builder().id(1L).name("USA").build();
        Region region = Region.builder().id(1L).name("California").country(country).build();

        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason = createSwellSeason("North Atlantic", seasonStart, seasonEnd);
        SurfSpot surfSpot = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot));

        assertFalse(notifications.isEmpty());
        NotificationDTO notification = notifications.get(0);
        assertEquals("North Atlantic", notification.getLocation());
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldUseRegionLocationWhenNoSeasonName() {
        Country country = Country.builder().id(1L).name("USA").build();
        Region region = Region.builder().id(1L).name("California").country(country).build();

        Month currentMonth = LocalDateTime.now().getMonth();
        String seasonStart = formatMonthName(currentMonth);
        String seasonEnd = formatMonthName(currentMonth.plus(3));

        SwellSeason swellSeason = createSwellSeason("", seasonStart, seasonEnd); // Empty name
        SurfSpot surfSpot = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot));

        assertFalse(notifications.isEmpty());
        NotificationDTO notification = notifications.get(0);
        assertNotNull(notification.getLocation());
        assertTrue(notification.getLocation().contains("USA") || 
                  notification.getLocation().contains("California"));
    }

    @Test
    void testGenerateSwellSeasonNotificationsShouldHandleInvalidMonthStrings() {
        Country country = Country.builder().id(1L).name("USA").build();
        Region region = Region.builder().id(1L).name("California").country(country).build();

        // Create season with invalid month string
        SwellSeason swellSeason = createSwellSeason("North Atlantic", "InvalidMonth", "April");
        SurfSpot surfSpot = createSurfSpotWithSeasonAndRegion("Mavericks", swellSeason, region);
        WatchListSurfSpot watchListSurfSpot = createWatchListSurfSpot(surfSpot);

        List<NotificationDTO> notifications = swellSeasonService.generateSwellSeasonNotifications(
            List.of(watchListSurfSpot));

        // Should not generate notifications with invalid month data
        assertTrue(notifications.isEmpty());
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
        return createSwellSeasonWithId(1L, name, start, end);
    }

    private SwellSeason createSwellSeasonWithId(Long id, String name, String start, String end) {
        SwellSeason season = new SwellSeason();
        season.setId(id);
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

