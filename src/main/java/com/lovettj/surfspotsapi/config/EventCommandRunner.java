package com.lovettj.surfspotsapi.config;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.lovettj.surfspotsapi.dto.ContestScheduleImportDTO;
import com.lovettj.surfspotsapi.service.ContestScheduleSyncService;
import com.lovettj.surfspotsapi.service.ContestVenueLinkService;
import com.lovettj.surfspotsapi.util.ContestVenueLocationKeyUtil;

@Component
@Profile("event-cli")
public class EventCommandRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(EventCommandRunner.class);

    private final ContestScheduleSyncService contestScheduleSyncService;
    private final ContestVenueLinkService contestVenueLinkService;
    private final ConfigurableApplicationContext applicationContext;

    public EventCommandRunner(
            ContestScheduleSyncService contestScheduleSyncService,
            ContestVenueLinkService contestVenueLinkService,
            ConfigurableApplicationContext applicationContext) {
        this.contestScheduleSyncService = contestScheduleSyncService;
        this.contestVenueLinkService = contestVenueLinkService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int exitCode = 0;
        try {
            if (args.containsOption("contest-sync")) {
                runSync(args);
            } else if (args.containsOption("contest-link")) {
                runLink(args);
            } else {
                printUsage();
                exitCode = 1;
            }
        } catch (Exception commandException) {
            logger.error("Event CLI command failed: {}", commandException.getMessage(), commandException);
            exitCode = 1;
        } finally {
            int finalExitCode = exitCode;
            System.exit(SpringApplication.exit(applicationContext, () -> finalExitCode));
        }
    }

    private void runSync(ApplicationArguments args) throws Exception {
        int year = resolveYear(args);
        Path scheduleFile = resolveScheduleFile(args);

        if (args.containsOption("dry-run")) {
            runSyncDryRun(scheduleFile, year);
            return;
        }

        ContestScheduleSyncService.ContestSyncResult result =
                contestScheduleSyncService.syncFromLocalFile(scheduleFile, year);
        logger.info(
                "Synced contest schedule for {}: created={}, updated={}, autoLinked={}",
                result.year(),
                result.createdCount(),
                result.updatedCount(),
                result.autoLinkedCount());
    }

    private void runSyncDryRun(Path scheduleFile, int year) throws Exception {
        ContestScheduleImportDTO schedule = contestScheduleSyncService.parseScheduleFromLocalFile(scheduleFile, year);

        logger.info(
                "Dry run — {} Championship Tour events for {} (no database changes):",
                schedule.getEvents().size(),
                schedule.getYear());
        for (ContestScheduleImportDTO.ContestScheduleEventDTO eventRow : schedule.getEvents()) {
            String venueKey = ContestVenueLocationKeyUtil.normalizeLocationKey(eventRow.getLocationName());
            logger.info(
                    "  {} | {} | {} | {} .. {} | {}",
                    venueKey,
                    eventRow.getName(),
                    eventRow.getLocationName(),
                    eventRow.getStartDate(),
                    eventRow.getEndDate(),
                    eventRow.getStatus() != null ? eventRow.getStatus() : "Scheduled");
        }
    }

    private Path resolveScheduleFile(ApplicationArguments args) {
        List<String> fileValues = args.getOptionValues("file");
        if (fileValues == null || fileValues.isEmpty() || fileValues.get(0).isBlank()) {
            throw new IllegalArgumentException(
                    "--file is required for --contest-sync. Save the CT schedule page in your browser first "
                            + "(see scripts/contest-import/README.md).");
        }
        return Path.of(fileValues.get(0));
    }

    private int resolveYear(ApplicationArguments args) {
        List<String> yearValues = args.getOptionValues("year");
        if (yearValues == null || yearValues.isEmpty() || yearValues.get(0).isBlank()) {
            return LocalDate.now().getYear();
        }
        try {
            return Integer.parseInt(yearValues.get(0));
        } catch (NumberFormatException parseException) {
            throw new IllegalArgumentException("--year must be a four-digit season year");
        }
    }

    private void runLink(ApplicationArguments args) {
        List<String> venueKeyValues = args.getOptionValues("venue-key");
        List<String> spotIdValues = args.getOptionValues("spot-id");

        if (venueKeyValues == null || venueKeyValues.isEmpty() || venueKeyValues.get(0).isBlank()) {
            throw new IllegalArgumentException("--venue-key is required for --contest-link");
        }
        if (spotIdValues == null || spotIdValues.isEmpty() || spotIdValues.get(0).isBlank()) {
            throw new IllegalArgumentException("--spot-id is required for --contest-link");
        }

        String venueKey = venueKeyValues.get(0);
        Long surfSpotId;
        try {
            surfSpotId = Long.parseLong(spotIdValues.get(0));
        } catch (NumberFormatException parseException) {
            throw new IllegalArgumentException("--spot-id must be a numeric surf spot ID");
        }
        ContestVenueLinkService.ContestLinkResult result =
                contestVenueLinkService.linkVenueToSurfSpot(venueKey, surfSpotId);
        logger.info(
                "Linked venue {} to surf spot {} ({} events)",
                result.venueLocationKey(),
                result.surfSpotId(),
                result.eventsUpdated());
    }

    private void printUsage() {
        logger.info("""
                Event CLI usage:
                  --contest-sync --file=./ct-2026.html [--year=2026] [--dry-run]
                  --contest-link --venue-key=punta-roca-la-libertad-el-salvador --spot-id=123
                Save the CT schedule page in your browser, then pass --file. Use --dry-run to preview without DB writes.
                Run with profile event-cli and web server disabled.
                """);
    }
}
