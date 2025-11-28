package com.lovettj.surfspotsapi.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lovettj.surfspotsapi.dto.NotificationDTO;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.SwellSeason;
import com.lovettj.surfspotsapi.entity.WatchListSurfSpot;
import com.lovettj.surfspotsapi.util.MonthUtils;

@Service
public class SwellSeasonService {

    /**
     * Generates swell season notifications for watched surf spots.
     * Groups notifications by region and swell season to avoid duplicates.
     * 
     * @param watchListSurfSpots List of watched surf spots
     * @return List of notifications for seasons starting or ending in the current month
     */
    public List<NotificationDTO> generateSwellSeasonNotifications(List<WatchListSurfSpot> watchListSurfSpots) {
        List<NotificationDTO> notifications = new ArrayList<>();

        if (watchListSurfSpots == null || watchListSurfSpots.isEmpty()) {
            return notifications;
        }

        Month currentMonth = LocalDateTime.now().getMonth();
        Map<String, List<SurfSpot>> regionSeasonGroups = new HashMap<>();

        // Group spots by region and swell season
        for (WatchListSurfSpot watchListSurfSpot : watchListSurfSpots) {
            SurfSpot surfSpot = watchListSurfSpot.getSurfSpot();
            if (surfSpot == null) {
                continue;
            }

            SwellSeason swellSeason = surfSpot.getSwellSeason();
            if (swellSeason == null) {
                continue;
            }

            Region region = surfSpot.getRegion();
            if (region == null) {
                continue;
            }

            // Check if season is starting or ending soon
            Month startMonth = MonthUtils.parseMonthString(swellSeason.getStartMonth());
            Month endMonth = MonthUtils.parseMonthString(swellSeason.getEndMonth());

            if (startMonth == null || endMonth == null) {
                continue;
            }

            boolean isSeasonStarting = currentMonth.equals(startMonth);
            // Check if current month is one month before the end month
            Month oneMonthBeforeEnd = endMonth.minus(1);
            boolean isSeasonEnding = currentMonth.equals(oneMonthBeforeEnd);

            if (!isSeasonStarting && !isSeasonEnding) {
                continue;
            }

            // Create a key for grouping: regionId + swellSeasonId
            String groupKey = region.getId() + "_" + swellSeason.getId();
            regionSeasonGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(surfSpot);
        }

        // Generate notifications for each group
        for (Map.Entry<String, List<SurfSpot>> entry : regionSeasonGroups.entrySet()) {
            List<SurfSpot> spots = entry.getValue();
            if (spots.isEmpty()) {
                continue;
            }

            SurfSpot firstSpot = spots.get(0);
            SwellSeason swellSeason = firstSpot.getSwellSeason();
            Region region = firstSpot.getRegion();

            Month startMonth = MonthUtils.parseMonthString(swellSeason.getStartMonth());
            Month endMonth = MonthUtils.parseMonthString(swellSeason.getEndMonth());
            Month currentMonthCheck = LocalDateTime.now().getMonth();
            
            boolean isSeasonStarting = currentMonthCheck.equals(startMonth);
            Month oneMonthBeforeEnd = endMonth.minus(1);
            boolean isSeasonEnding = currentMonthCheck.equals(oneMonthBeforeEnd);

            // Build notification
            String seasonName = swellSeason.getName();
            String title;
            if (isSeasonStarting) {
                title = seasonName + " Has Arrived";
            } else if (isSeasonEnding) {
                title = seasonName + " Ending Soon";
            } else {
                // Should not happen since we filter for starting/ending, but handle gracefully
                title = seasonName;
            }

            // Use swell season name as location, or fall back to region/country
            String location = (seasonName != null && !seasonName.trim().isEmpty())
                ? seasonName
                : buildLocationString(region);

            // Build description with affected surf spots
            String description = buildDescription(spots, isSeasonStarting);

            NotificationDTO notification = NotificationDTO.builder()
                .type("swell")
                .title(title)
                .description(description)
                .location(location)
                .createdAt(LocalDateTime.now())
                .build();

            notifications.add(notification);
        }

        return notifications;
    }

    /**
     * Builds a location string from region and country information
     */
    private String buildLocationString(Region region) {
        if (region == null) {
            return "";
        }

        StringBuilder location = new StringBuilder();
        if (region.getCountry() != null && region.getCountry().getName() != null) {
            location.append(region.getCountry().getName());
        }
        if (region.getName() != null) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(region.getName());
        }
        return location.toString();
    }

    /**
     * Builds an enticing description about the affected surf spots.
     * Lists up to MAX_SPOTS_TO_LIST spots, then shows a count for the rest.
     */
    private static final int MAX_SPOTS_TO_LIST = 3;

    private String buildDescription(List<SurfSpot> spots, boolean isSeasonStarting) {
        if (spots == null || spots.isEmpty()) {
            return "";
        }

        int spotCount = spots.size();
        List<String> validSpotNames = spots.stream()
            .map(SurfSpot::getName)
            .filter(name -> name != null && !name.trim().isEmpty())
            .collect(Collectors.toList());

        if (validSpotNames.isEmpty()) {
            return buildGenericDescription(isSeasonStarting, spotCount);
        }

        if (isSeasonStarting) {
            return buildStartingDescription(validSpotNames, spotCount);
        } else {
            return buildEndingDescription(validSpotNames, spotCount);
        }
    }

    /**
     * Builds an enticing description for when a season is starting
     */
    private String buildStartingDescription(List<String> spotNames, int totalCount) {
        if (totalCount == 1) {
            return String.format("The prime swell season has arrived for %s! Perfect conditions are on the way - time to check the forecast and plan your session.", 
                spotNames.get(0));
        }

        int spotsToShow = Math.min(MAX_SPOTS_TO_LIST, spotNames.size());
        String listedSpots = String.join(", ", spotNames.subList(0, spotsToShow));
        
        if (totalCount <= MAX_SPOTS_TO_LIST) {
            return String.format("The prime swell season has arrived! Your watched spots including %s are about to light up with epic conditions. Check the forecasts and start planning your trip!", 
                listedSpots);
        } else {
            int remainingCount = totalCount - spotsToShow;
            return String.format("The prime swell season has arrived! %s and %d more of your watched spots are about to light up with epic conditions. Perfect time to check forecasts and book that last-minute trip!", 
                listedSpots, remainingCount);
        }
    }

    /**
     * Builds an enticing description for when a season is ending soon
     */
    private String buildEndingDescription(List<String> spotNames, int totalCount) {
        if (totalCount == 1) {
            return String.format("The prime swell season for %s is ending soon! Don't miss out on these last epic sessions - check the forecast and get out there while conditions are still firing.", 
                spotNames.get(0));
        }

        int spotsToShow = Math.min(MAX_SPOTS_TO_LIST, spotNames.size());
        String listedSpots = String.join(", ", spotNames.subList(0, spotsToShow));
        
        if (totalCount <= MAX_SPOTS_TO_LIST) {
            return String.format("The prime swell season is ending soon! Your watched spots including %s still have great conditions. Check the forecasts and catch these last epic sessions before the season wraps up!", 
                listedSpots);
        } else {
            int remainingCount = totalCount - spotsToShow;
            return String.format("The prime swell season is ending soon! %s and %d more of your watched spots still have great conditions. Don't miss out - check forecasts and book that last-minute trip before the season ends!", 
                listedSpots, remainingCount);
        }
    }

    /**
     * Builds a generic description when spot names aren't available
     */
    private String buildGenericDescription(boolean isSeasonStarting, int spotCount) {
        if (isSeasonStarting) {
            return String.format("The prime swell season has arrived for %d of your watched spots! Perfect conditions are on the way - time to check the forecast and plan your sessions.", 
                spotCount);
        } else {
            return String.format("The prime swell season is ending soon for %d of your watched spots! Don't miss out on these last epic sessions - check the forecast and get out there while conditions are still firing.", 
                spotCount);
        }
    }
}
