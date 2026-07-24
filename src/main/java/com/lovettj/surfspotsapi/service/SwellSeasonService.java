package com.lovettj.surfspotsapi.service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    private static final int MAX_SPOTS_TO_LIST = 3;

    /**
     * Generates swell season notifications for watched surf spots.
     * Groups by region and swell season. Only fires when the season starts
     * this month, or ends next month.
     */
    public List<NotificationDTO> generateSwellSeasonNotifications(List<WatchListSurfSpot> watchListSurfSpots) {
        List<NotificationDTO> notifications = new ArrayList<>();

        if (watchListSurfSpots == null || watchListSurfSpots.isEmpty()) {
            return notifications;
        }

        Month currentMonth = LocalDateTime.now().getMonth();
        Map<String, List<SurfSpot>> regionSeasonGroups = new HashMap<>();

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

            Month startMonth = MonthUtils.parseMonthString(swellSeason.getStartMonth());
            Month endMonth = MonthUtils.parseMonthString(swellSeason.getEndMonth());
            if (startMonth == null || endMonth == null) {
                continue;
            }

            boolean isSeasonStarting = currentMonth.equals(startMonth);
            boolean isSeasonEnding = currentMonth.equals(endMonth.minus(1));
            if (!isSeasonStarting && !isSeasonEnding) {
                continue;
            }

            String groupKey = region.getId() + "_" + swellSeason.getId();
            regionSeasonGroups.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(surfSpot);
        }

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
            boolean isSeasonStarting = currentMonth.equals(startMonth);

            String seasonName =
                    swellSeason.getName() != null && !swellSeason.getName().isBlank()
                            ? swellSeason.getName().trim()
                            : "Swell season";
            String regionLabel = buildLocationString(region);
            String seasonWindow = formatSeasonWindow(startMonth, endMonth);
            String title =
                    isSeasonStarting
                            ? seasonName + " is starting"
                            : seasonName + " ends next month";
            String description =
                    buildDescription(spots, isSeasonStarting, regionLabel, seasonWindow);

            String phase = isSeasonStarting ? "starting" : "ending";
            int seasonYear = LocalDateTime.now().getYear();

            notifications.add(
                    NotificationDTO.builder()
                            .id("swell-" + entry.getKey() + "-" + seasonYear + "-" + phase)
                            .type("swell")
                            .title(title)
                            .description(description)
                            .location(regionLabel.isBlank() ? seasonName : regionLabel)
                            .surfSpotName(firstSpot.getName())
                            .link("/watch-list")
                            .createdAt(LocalDateTime.now())
                            .build());
        }

        return notifications;
    }

    private String buildLocationString(Region region) {
        if (region == null) {
            return "";
        }
        StringBuilder location = new StringBuilder();
        if (region.getName() != null && !region.getName().isBlank()) {
            location.append(region.getName().trim());
        }
        if (region.getCountry() != null
                && region.getCountry().getName() != null
                && !region.getCountry().getName().isBlank()) {
            if (location.length() > 0) {
                location.append(", ");
            }
            location.append(region.getCountry().getName().trim());
        }
        return location.toString();
    }

    private static String formatSeasonWindow(Month startMonth, Month endMonth) {
        if (startMonth == null || endMonth == null) {
            return "";
        }
        return displayMonth(startMonth) + " to " + displayMonth(endMonth);
    }

    private static String displayMonth(Month month) {
        return month.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private String buildDescription(
            List<SurfSpot> spots,
            boolean isSeasonStarting,
            String regionLabel,
            String seasonWindow) {
        List<String> spotNames =
                spots.stream()
                        .map(SurfSpot::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toList());

        String where =
                regionLabel == null || regionLabel.isBlank() ? "spots you watch" : regionLabel;
        String windowPart =
                seasonWindow == null || seasonWindow.isBlank() ? "" : " (" + seasonWindow + ")";
        String spotsPart = formatSpotList(spotNames, spots.size());

        if (isSeasonStarting) {
            return "Prime season for "
                    + where
                    + windowPart
                    + " starts this month"
                    + spotsPart
                    + " Check forecasts and plan sessions or a trip while the window is open.";
        }
        return "Prime season for "
                + where
                + windowPart
                + " ends next month"
                + spotsPart
                + " Get out there before conditions drop off.";
    }

    private static String formatSpotList(List<String> spotNames, int totalCount) {
        if (spotNames.isEmpty()) {
            if (totalCount <= 0) {
                return ".";
            }
            return " across " + totalCount + " watched spot" + (totalCount == 1 ? "" : "s") + ".";
        }
        int spotsToShow = Math.min(MAX_SPOTS_TO_LIST, spotNames.size());
        String listed = String.join(", ", spotNames.subList(0, spotsToShow));
        if (totalCount <= MAX_SPOTS_TO_LIST) {
            return ": " + listed + ".";
        }
        int remaining = totalCount - spotsToShow;
        return ": " + listed + ", and " + remaining + " more.";
    }
}
