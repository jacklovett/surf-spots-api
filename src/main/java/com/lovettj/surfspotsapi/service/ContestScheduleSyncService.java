package com.lovettj.surfspotsapi.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
import com.lovettj.surfspotsapi.dto.ContestScheduleImportDTO;
import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.entity.SurfEventContestDetail;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EventSource;
import com.lovettj.surfspotsapi.enums.EventStatus;
import com.lovettj.surfspotsapi.enums.EventType;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.util.ContestScheduleHtmlParser;
import com.lovettj.surfspotsapi.util.ContestVenueLocationKeyUtil;

@Service
public class ContestScheduleSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ContestScheduleSyncService.class);

    private final SurfEventRepository surfEventRepository;
    private final SurfSpotRepository surfSpotRepository;

    public ContestScheduleSyncService(SurfEventRepository surfEventRepository, SurfSpotRepository surfSpotRepository) {
        this.surfEventRepository = surfEventRepository;
        this.surfSpotRepository = surfSpotRepository;
    }

    /**
     * Parse a CT schedule page saved locally without writing to the database.
     */
    public ContestScheduleImportDTO parseScheduleFromLocalFile(Path htmlFile, int year) throws IOException {
        if (htmlFile == null) {
            throw new IllegalArgumentException("Schedule file path is required");
        }
        if (!Files.isRegularFile(htmlFile)) {
            throw new IllegalArgumentException("Schedule file not found: " + htmlFile);
        }
        String html = Files.readString(htmlFile);
        ContestScheduleImportDTO schedule = ContestScheduleHtmlParser.parseScheduleHtml(html, year);
        logger.info("Parsed contest schedule from local file {} ({} events)", htmlFile, schedule.getEvents().size());
        return schedule;
    }

    /**
     * Upsert events from a browser-saved schedule file. Does not contact worldsurfleague.com.
     */
    @Transactional
    public ContestSyncResult syncFromLocalFile(Path htmlFile, int year) throws IOException {
        ContestScheduleImportDTO schedule = parseScheduleFromLocalFile(htmlFile, year);
        return upsertSchedule(schedule);
    }

    /** Upsert events from a parsed schedule DTO. Used by sync and integration tests. */
    @Transactional
    public ContestSyncResult upsertSchedule(ContestScheduleImportDTO schedule) {
        if (schedule.getYear() == null) {
            throw new IllegalArgumentException("Contest schedule must include year");
        }
        if (schedule.getEvents() == null || schedule.getEvents().isEmpty()) {
            throw new IllegalArgumentException("Contest schedule must include at least one event");
        }

        int createdCount = 0;
        int updatedCount = 0;
        int autoLinkedCount = 0;

        for (ContestScheduleImportDTO.ContestScheduleEventDTO eventRow : schedule.getEvents()) {
            validateEventRow(eventRow);

            String venueLocationKey = ContestVenueLocationKeyUtil.normalizeLocationKey(eventRow.getLocationName());
            Optional<SurfEvent> existingOptional = surfEventRepository.findContestByOrganizerSeriesVenueAndSeasonYear(
                    ContestImportConstants.ORGANIZER_WSL,
                    ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                    venueLocationKey,
                    schedule.getYear());

            if (existingOptional.isPresent()) {
                SurfEvent existing = existingOptional.get();
                applyScheduleRowToEvent(existing, eventRow);
                surfEventRepository.save(existing);
                updatedCount++;
            } else {
                SurfEvent created = buildContestEventFromScheduleRow(eventRow, venueLocationKey, schedule.getYear());
                if (autoCarrySurfSpotLink(created, venueLocationKey, schedule.getYear())) {
                    autoLinkedCount++;
                }
                surfEventRepository.save(created);
                createdCount++;
            }
        }

        ContestSyncResult result = new ContestSyncResult(schedule.getYear(), createdCount, updatedCount, autoLinkedCount);
        logger.info(
                "Contest schedule sync complete for year {}: {} created, {} updated, {} auto-linked",
                result.year(),
                result.createdCount(),
                result.updatedCount(),
                result.autoLinkedCount());
        return result;
    }

    private SurfEvent buildContestEventFromScheduleRow(
            ContestScheduleImportDTO.ContestScheduleEventDTO eventRow, String venueLocationKey, int seasonYear) {
        SurfEvent event = SurfEvent.builder()
                .eventType(EventType.CONTEST)
                .name(eventRow.getName())
                .locationName(eventRow.getLocationName())
                .startDate(eventRow.getStartDate())
                .endDate(eventRow.getEndDate())
                .status(EventStatus.fromContestPageLabel(eventRow.getStatus()))
                .source(EventSource.CONTEST_HTML_IMPORT)
                .build();

        SurfEventContestDetail contestDetail = SurfEventContestDetail.builder()
                .organizer(ContestImportConstants.ORGANIZER_WSL)
                .series(ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES)
                .seasonYear(seasonYear)
                .venueLocationKey(venueLocationKey)
                .url(eventRow.getUrl())
                .build();
        event.setContestDetail(contestDetail);
        return event;
    }

    private void applyScheduleRowToEvent(
            SurfEvent event, ContestScheduleImportDTO.ContestScheduleEventDTO eventRow) {
        event.setName(eventRow.getName());
        event.setLocationName(eventRow.getLocationName());
        event.setStartDate(eventRow.getStartDate());
        event.setEndDate(eventRow.getEndDate());
        event.setStatus(EventStatus.fromContestPageLabel(eventRow.getStatus()));
        event.getContestDetail().setUrl(eventRow.getUrl());
    }

    private void validateEventRow(ContestScheduleImportDTO.ContestScheduleEventDTO eventRow) {
        List<String> missingFields = new ArrayList<>();
        if (eventRow.getName() == null || eventRow.getName().isBlank()) {
            missingFields.add("name");
        }
        if (eventRow.getLocationName() == null || eventRow.getLocationName().isBlank()) {
            missingFields.add("locationName");
        }
        if (eventRow.getStartDate() == null) {
            missingFields.add("startDate");
        }
        if (eventRow.getEndDate() == null) {
            missingFields.add("endDate");
        }
        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Event row missing required fields: " + String.join(", ", missingFields));
        }
        if (eventRow.getEndDate().isBefore(eventRow.getStartDate())) {
            throw new IllegalArgumentException(
                    "endDate must be on or after startDate for event: " + eventRow.getName());
        }
    }

    private boolean autoCarrySurfSpotLink(SurfEvent event, String venueLocationKey, int seasonYear) {
        Optional<SurfEvent> priorSeasonEventOptional = surfEventRepository
                .findFirstByContestDetailOrganizerAndContestDetailSeriesAndContestDetailVenueLocationKeyAndContestDetailSeasonYearLessThanOrderByContestDetailSeasonYearDesc(
                        ContestImportConstants.ORGANIZER_WSL,
                        ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                        venueLocationKey,
                        seasonYear);

        if (priorSeasonEventOptional.isEmpty() || priorSeasonEventOptional.get().getSurfSpot() == null) {
            return false;
        }

        SurfSpot linkedSpot = priorSeasonEventOptional.get().getSurfSpot();
        event.setSurfSpot(linkedSpot);
        if (!Boolean.TRUE.equals(linkedSpot.getIsWslTourStop())) {
            linkedSpot.setIsWslTourStop(true);
            surfSpotRepository.save(linkedSpot);
        }
        return true;
    }

    public record ContestSyncResult(int year, int createdCount, int updatedCount, int autoLinkedCount) {}
}
