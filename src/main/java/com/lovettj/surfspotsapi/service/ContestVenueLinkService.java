package com.lovettj.surfspotsapi.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lovettj.surfspotsapi.constants.ContestImportConstants;
import com.lovettj.surfspotsapi.entity.SurfEvent;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.repository.SurfEventRepository;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.util.ContestVenueLocationKeyUtil;

@Service
public class ContestVenueLinkService {

    private static final Logger logger = LoggerFactory.getLogger(ContestVenueLinkService.class);

    private final SurfEventRepository surfEventRepository;
    private final SurfSpotRepository surfSpotRepository;

    public ContestVenueLinkService(SurfEventRepository surfEventRepository, SurfSpotRepository surfSpotRepository) {
        this.surfEventRepository = surfEventRepository;
        this.surfSpotRepository = surfSpotRepository;
    }

    @Transactional
    public ContestLinkResult linkVenueToSurfSpot(String venueLocationKey, Long surfSpotId) {
        if (venueLocationKey == null || venueLocationKey.isBlank()) {
            throw new IllegalArgumentException("venueLocationKey is required");
        }
        if (surfSpotId == null) {
            throw new IllegalArgumentException("surfSpotId is required");
        }

        String normalizedVenueKey = ContestVenueLocationKeyUtil.normalizeLocationKey(venueLocationKey);

        SurfSpot surfSpot = surfSpotRepository
                .findById(surfSpotId)
                .orElseThrow(() -> new IllegalArgumentException("Surf spot not found: " + surfSpotId));

        List<SurfEvent> venueEvents = surfEventRepository.findContestsByOrganizerSeriesAndVenue(
                ContestImportConstants.ORGANIZER_WSL,
                ContestImportConstants.CHAMPIONSHIP_TOUR_SERIES,
                normalizedVenueKey);
                
        if (venueEvents.isEmpty()) {
            throw new IllegalArgumentException(
                    "No contest events found for venue key: " + normalizedVenueKey);
        }

        for (SurfEvent event : venueEvents) {
            event.setSurfSpot(surfSpot);
        }
        surfEventRepository.saveAll(venueEvents);

        surfSpot.setIsWslTourStop(true);
        surfSpotRepository.save(surfSpot);

        logger.info(
                "Linked venue {} to surf spot {} ({} events updated)",
                normalizedVenueKey,
                surfSpotId,
                venueEvents.size());

        return new ContestLinkResult(normalizedVenueKey, surfSpotId, venueEvents.size());
    }

    public record ContestLinkResult(String venueLocationKey, Long surfSpotId, int eventsUpdated) {}
}
