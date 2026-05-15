package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionMediaDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.dto.UserSurfSessionsDTO;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSessionMedia;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.Surfboard;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.ExternalSessionProvider;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.repository.SurfSessionMediaRepository;
import com.lovettj.surfspotsapi.repository.SurfSessionRepository;
import com.lovettj.surfspotsapi.util.SurfSpotPathUtil;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.SurfboardRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.CreateSurfSessionMediaRequest;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import com.lovettj.surfspotsapi.util.SqlExceptionInspection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SurfSessionService {
    private static final Logger logger = LoggerFactory.getLogger(SurfSessionService.class);
    private static final int MIN_SAMPLE_FOR_SKILL_SEGMENT = 3;
    private static final int MAX_SESSION_DURATION_MINUTES = 24 * 60;

    private record ResolvedTiming(
            LocalDate sessionDate, Integer durationMinutes, Instant sessionStartInstant, Instant sessionEndInstant) {}

    private final SurfSessionRepository surfSessionRepository;
    private final SurfSessionMediaRepository surfSessionMediaRepository;
    private final SurfSpotRepository surfSpotRepository;
    private final UserRepository userRepository;
    private final SurfboardRepository surfboardRepository;
    private final UserSurfSpotService userSurfSpotService;
    private final StorageService storageService;

    public SurfSessionService(
            SurfSessionRepository surfSessionRepository,
            SurfSessionMediaRepository surfSessionMediaRepository,
            SurfSpotRepository surfSpotRepository,
            UserRepository userRepository,
            SurfboardRepository surfboardRepository,
            UserSurfSpotService userSurfSpotService,
            StorageService storageService) {
        this.surfSessionRepository = surfSessionRepository;
        this.surfSessionMediaRepository = surfSessionMediaRepository;
        this.surfSpotRepository = surfSpotRepository;
        this.userRepository = userRepository;
        this.surfboardRepository = surfboardRepository;
        this.userSurfSpotService = userSurfSpotService;
        this.storageService = storageService;
    }

    @Transactional
    public void createSession(SurfSessionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.USER_NOT_FOUND));

        SurfSpot surfSpot = surfSpotRepository.findById(request.getSurfSpotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.SURF_SPOT_NOT_FOUND));

        SkillLevel userSkillLevel = user.getSkillLevel();
        if (userSkillLevel == null) {
            userSkillLevel = request.getSkillLevel();
            if (userSkillLevel == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SKILL_LEVEL_REQUIRED_FOR_SESSION);
            }
            user.setSkillLevel(userSkillLevel);
            userRepository.save(user);
        }

        Surfboard surfboard = null;
        if (request.getSurfboardId() != null && !request.getSurfboardId().isBlank()) {
            surfboard = surfboardRepository
                    .findByIdAndUserId(request.getSurfboardId().trim(), request.getUserId())
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SURFBOARD_NOT_FOUND_FOR_USER));
        }

        Boolean wouldAgain = request.getWouldSurfAgain();
        if (wouldAgain == null) {
            wouldAgain = Boolean.FALSE;
        }

        ResolvedTiming timing = resolveTiming(request, surfSpot);

        ExternalSyncPair externalSync = resolveExternalSyncPair(request);
        if (externalSync != null
                && surfSessionRepository.existsByUserIdAndExternalSessionProviderAndExternalSessionId(
                        request.getUserId(), externalSync.provider(), externalSync.externalId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ApiErrors.SURF_SESSION_ALREADY_SYNCED);
        }

        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(userSkillLevel)
                .sessionDate(timing.sessionDate())
                .durationMinutes(timing.durationMinutes())
                .sessionStartInstant(timing.sessionStartInstant())
                .sessionEndInstant(timing.sessionEndInstant())
                .externalSessionProvider(externalSync != null ? externalSync.provider() : null)
                .externalSessionId(externalSync != null ? externalSync.externalId() : null)
                .waveSize(request.getWaveSize())
                .crowdLevel(request.getCrowdLevel())
                .waveQuality(request.getWaveQuality())
                .swellDirection(blankToNull(request.getSwellDirection()))
                .windDirection(blankToNull(request.getWindDirection()))
                .tide(request.getTide())
                .sessionNotes(blankToNull(request.getSessionNotes()))
                .wouldSurfAgain(wouldAgain)
                .surfboard(surfboard)
                .build();

        persistSessionOrConflictOnDuplicateExternalId(session, externalSync != null);

        // Idempotent: ensures the spot appears in surfed spots without a separate "I surfed here" step.
        userSurfSpotService.addUserSurfSpot(request.getUserId(), request.getSurfSpotId());
    }

    /**
     * Persists the session; maps PostgreSQL unique violations on concurrent duplicate imports to HTTP 409.
     */
    private void persistSessionOrConflictOnDuplicateExternalId(SurfSession session, boolean hasExternalSyncPair) {
        try {
            surfSessionRepository.save(session);
            // Force INSERT while still in this try/catch so unique violations are not deferred to commit (500 path).
            surfSessionRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (hasExternalSyncPair && SqlExceptionInspection.isSurfSessionExternalSyncUniqueViolation(exception)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ApiErrors.SURF_SESSION_ALREADY_SYNCED);
            }
            throw exception;
        }
    }

    /**
     * Sessions page for a user: DB-backed headline stats plus the full session list (newest first),
     * same bundle pattern as user-spots / watch list.
     */
    public UserSurfSessionsDTO getSurfSessionsForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_SUMMARY_USER_ID_REQUIRED);
        }
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.USER_NOT_FOUND);
        }
        List<SurfSessionListItemDTO> sessions = surfSessionRepository.findAllForUserList(userId).stream()
                .map(this::toListItem)
                .toList();
        sessions.forEach(this::applySignedMediaUrls);
        return UserSurfSessionsDTO.builder()
                .totalSessions(surfSessionRepository.countAllByUserId(userId))
                .spotsSurfedCount(surfSessionRepository.countDistinctSurfSpotsByUserId(userId))
                .boardsUsedCount(surfSessionRepository.countDistinctBoardsByUserId(userId))
                .sessions(sessions)
                .build();
    }

    private SurfSessionListItemDTO toListItem(SurfSession s) {
        SurfSpot spot = s.getSurfSpot();
        ZoneId zone = zoneForSpotDisplay(spot);
        String spotPath = SurfSpotPathUtil.pathFor(spot);
        Surfboard board = s.getSurfboard();
        List<SurfSessionMediaDTO> mediaDtos =
                s.getMedia() == null || s.getMedia().isEmpty()
                        ? List.of()
                        : s.getMedia().stream().map(SurfSessionMediaDTO::new).toList();
        return SurfSessionListItemDTO.builder()
                .id(s.getId())
                .sessionDate(s.getSessionDate())
                .durationMinutes(s.getDurationMinutes())
                .sessionStartTime(localTimeAtZone(s.getSessionStartInstant(), zone))
                .sessionEndTime(localTimeAtZone(s.getSessionEndInstant(), zone))
                .sessionStartInstant(s.getSessionStartInstant())
                .sessionEndInstant(s.getSessionEndInstant())
                .externalSessionProvider(s.getExternalSessionProvider())
                .externalSessionId(s.getExternalSessionId())
                .createdAt(s.getCreatedAt())
                .surfSpotId(spot.getId())
                .surfSpotName(spot.getName())
                .spotPath(spotPath)
                .waveSize(s.getWaveSize())
                .crowdLevel(s.getCrowdLevel())
                .waveQuality(s.getWaveQuality())
                .swellDirection(s.getSwellDirection())
                .windDirection(s.getWindDirection())
                .tide(s.getTide())
                .sessionNotes(s.getSessionNotes())
                .wouldSurfAgain(s.getWouldSurfAgain())
                .skillLevel(s.getSkillLevel())
                .surfboardId(board != null ? board.getId() : null)
                .surfboardName(board != null ? board.getName() : null)
                .media(mediaDtos)
                .build();
    }

    private static LocalTime localTimeAtZone(Instant instant, ZoneId zone) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(zone).toLocalTime();
    }

    private void applySignedMediaUrls(SurfSessionListItemDTO sessionDto) {
        if (sessionDto == null || sessionDto.getMedia() == null || sessionDto.getMedia().isEmpty() || !storageService.isStorageConfigured()) {
            return;
        }

        sessionDto.getMedia().forEach(mediaDto -> {
            String mediaType = mediaDto.getMediaType() != null ? mediaDto.getMediaType() : "image";
            String resolvedKey = storageService.resolveObjectKeyWithFallback(
                    null,
                    mediaDto.getOriginalUrl(),
                    mediaDto.getId(),
                    mediaType,
                    "surf-sessions/media");
                    
            try {
                String signedUrl = storageService.generatePresignedDownloadUrl(resolvedKey);
                mediaDto.setOriginalUrl(signedUrl);
                mediaDto.setThumbUrl(signedUrl);
            } catch (Exception exception) {
                logger.warn("Failed to presign surf session media URL. mediaId={}, keeping original URL.", mediaDto.getId(), exception);
            }
        });
    }

    public String getUploadUrl(String userId, Long sessionId, String mediaType, String mediaId) {
        SurfSession session = surfSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.SURF_SESSION_NOT_FOUND));
        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.SURF_SESSION_MEDIA_ADD_FORBIDDEN);
        }
        if (mediaType == null || (!mediaType.equals("image") && !mediaType.equals("video"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.MEDIA_TYPE_MUST_BE_IMAGE_OR_VIDEO);
        }
        String contentType = "image".equals(mediaType) ? "image/jpeg" : "video/mp4";
        String s3Key = storageService.generateMediaKey(mediaId, mediaType, "surf-sessions/media");
        return storageService.generatePresignedUploadUrl(s3Key, contentType);
    }

    @Transactional
    public SurfSessionMediaDTO addMedia(String userId, Long sessionId, CreateSurfSessionMediaRequest request) {
        SurfSession session = surfSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.SURF_SESSION_NOT_FOUND));
        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.SURF_SESSION_MEDIA_ADD_FORBIDDEN);
        }
        String mediaType = request.getMediaType() != null ? request.getMediaType() : "image";
        String mediaId = request.getMediaId() != null && !request.getMediaId().isBlank()
                ? request.getMediaId()
                : UUID.randomUUID().toString();
        String objectKey = storageService.generateMediaKey(mediaId, mediaType, "surf-sessions/media");
        SurfSessionMedia media = SurfSessionMedia.builder()
                .id(mediaId)
                .surfSession(session)
                .originalUrl(request.getOriginalUrl())
                .thumbUrl(request.getThumbUrl())
                .objectKey(objectKey)
                .mediaType(mediaType)
                .build();
        media = surfSessionMediaRepository.save(media);
        return new SurfSessionMediaDTO(media);
    }

    @Transactional
    public void deleteMedia(String userId, String mediaId) {
        SurfSessionMedia media = surfSessionMediaRepository
                .findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.MEDIA_NOT_FOUND));
        if (!media.getSurfSession().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.MEDIA_DELETE_FORBIDDEN);
        }
        String mediaType = media.getMediaType() != null ? media.getMediaType() : "image";
        String objectKey = storageService.resolveObjectKeyWithFallback(
                media.getObjectKey(),
                media.getOriginalUrl(),
                media.getId(),
                mediaType,
                "surf-sessions/media");
        storageService.deleteObject(objectKey);
        surfSessionMediaRepository.delete(media);
    }

    /**
     * Session summary for a spot, segmented by the user's skill level from the database.
     * {@code userId} is required (also enforced by the controller query parameter).
     */
    public SurfSessionSummaryDTO getSpotSummaryForUser(Long surfSpotId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_SUMMARY_USER_ID_REQUIRED);
        }
        String trimmed = userId.trim();
        User user = userRepository.findById(trimmed)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.USER_NOT_FOUND));
        return getSpotSummary(surfSpotId, user.getSkillLevel());
    }

    public SurfSessionSummaryDTO getSpotSummary(Long surfSpotId, SkillLevel skillLevel) {
        List<SurfSession> sessions = skillLevel == null
                ? surfSessionRepository.findBySurfSpotId(surfSpotId)
                : surfSessionRepository.findBySurfSpotIdAndSkillLevel(surfSpotId, skillLevel);

        boolean fallbackToAllSkills = false;
        SkillLevel effectiveSkillLevel = skillLevel;
        if (skillLevel != null && sessions.size() < MIN_SAMPLE_FOR_SKILL_SEGMENT) {
            sessions = surfSessionRepository.findBySurfSpotId(surfSpotId);
            fallbackToAllSkills = true;
            effectiveSkillLevel = null;
        }

        long trueCount =
                sessions.stream().filter(s -> Boolean.TRUE.equals(s.getWouldSurfAgain())).count();
        long falseCount =
                sessions.stream().filter(s -> Boolean.FALSE.equals(s.getWouldSurfAgain())).count();

        Map<String, Long> waveQualityDistribution =
                countBy(sessions, s -> s.getWaveQuality() != null ? s.getWaveQuality().name() : null);
        Map<String, Long> crowdDistribution =
                countBy(sessions, s -> s.getCrowdLevel() != null ? s.getCrowdLevel().name() : null);

        return SurfSessionSummaryDTO.builder()
                .skillLevel(effectiveSkillLevel)
                .sampleSize(sessions.size())
                .waveSizeDistribution(countBy(sessions, s -> s.getWaveSize() != null ? s.getWaveSize().name() : null))
                .crowdDistribution(crowdDistribution)
                .waveQualityDistribution(waveQualityDistribution)
                .wouldSurfAgainTrueCount(trueCount)
                .wouldSurfAgainFalseCount(falseCount)
                .fallbackToAllSkills(fallbackToAllSkills)
                .segmentHeadline(buildSegmentHeadline(effectiveSkillLevel, sessions.size(), fallbackToAllSkills))
                .waveQualityTrendLine(waveQualityTrendLineFromDistribution(waveQualityDistribution))
                .crowdTrendLine(crowdTrendLineFromDistribution(crowdDistribution))
                .wouldSurfAgainLine(buildWouldSurfAgainLine(trueCount, falseCount))
                .build();
    }

    private static String buildSegmentHeadline(
            SkillLevel skillLevel, int sampleSize, boolean fallbackToAllSkills) {
        if (fallbackToAllSkills || skillLevel == null) {
            return String.format("Surfers (%d sessions)", sampleSize);
        }
        return String.format("%s (%d sessions)", skillLevel.getDisplayName(), sampleSize);
    }

    private static String buildWouldSurfAgainLine(long trueCount, long falseCount) {
        long total = trueCount + falseCount;
        if (total <= 0) {
            return null;
        }
        return String.format("%d/%d would surf again", trueCount, total);
    }

    /**
     * Bucket with the highest count (ties broken by map iteration order for equal counts).
     */
    private static String dominantEnumKey(Map<String, Long> countsByEnumName) {
        if (countsByEnumName == null || countsByEnumName.isEmpty()) {
            return null;
        }
        return Collections.max(countsByEnumName.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private static String waveQualityTrendLineFromDistribution(Map<String, Long> countsByEnumName) {
        String topKey = dominantEnumKey(countsByEnumName);
        if (topKey == null) {
            return null;
        }
        return WaveQuality.valueOf(topKey).getSummaryTrendLine();
    }

    private static String crowdTrendLineFromDistribution(Map<String, Long> countsByEnumName) {
        String topKey = dominantEnumKey(countsByEnumName);
        if (topKey == null) {
            return null;
        }
        return CrowdLevel.valueOf(topKey).getSummaryTrendLine();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Both external fields null for manual logs; otherwise both required (see {@link ApiErrors#EXTERNAL_SESSION_SYNC_PAIR_REQUIRED}).
     */
    private ExternalSyncPair resolveExternalSyncPair(SurfSessionRequest request) {
        String externalId = blankToNull(request.getExternalSessionId());
        ExternalSessionProvider provider = request.getExternalSessionProvider();
        if (externalId == null && provider == null) {
            return null;
        }
        if (externalId == null || provider == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.EXTERNAL_SESSION_SYNC_PAIR_REQUIRED);
        }
        return new ExternalSyncPair(provider, externalId);
    }

    private record ExternalSyncPair(ExternalSessionProvider provider, String externalId) {}

    /**
     * Resolves timing from wearable/partner instants (authoritative) or manual local time fields.
     * Local calendar fields for instant-based sessions are derived using the surf spot's {@link SurfSpot#getIanaZoneId()}
     * when set; otherwise UTC.
     */
    private ResolvedTiming resolveTiming(SurfSessionRequest request, SurfSpot surfSpot) {
        boolean usesInstants =
                request.getSessionStartInstant() != null || request.getSessionEndInstant() != null;
        if (usesInstants) {
            return resolveFromInstants(request, surfSpot);
        }
        LocalDate sessionDate = request.getSessionDate();
        if (sessionDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_DATE_OR_START_INSTANT_REQUIRED);
        }
        return resolveFromManualLocalTimes(request, surfSpot, sessionDate);
    }

    private ZoneId zoneForSpotDisplay(SurfSpot surfSpot) {
        String raw = surfSpot.getIanaZoneId();
        if (raw != null && !raw.isBlank()) {
            try {
                return ZoneId.of(raw.trim());
            } catch (DateTimeException ex) {
                logger.warn("Invalid iana_zone_id on surf spot {}: {}", surfSpot.getId(), raw);
            }
        }
        return ZoneId.of("UTC");
    }

    private ResolvedTiming resolveFromInstants(SurfSessionRequest request, SurfSpot surfSpot) {
        Instant start = request.getSessionStartInstant();
        Instant end = request.getSessionEndInstant();
        ZoneId zone = zoneForSpotDisplay(surfSpot);

        boolean hasStart = start != null;
        boolean hasEnd = end != null;

        if (hasEnd && !hasStart) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_END_TIME_REQUIRES_START);
        }

        if (!hasStart && !hasEnd) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_DATE_OR_START_INSTANT_REQUIRED);
        }

        if (hasStart && hasEnd) {
            if (!end.isAfter(start)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_END_BEFORE_START);
            }
            long betweenMinutes = ChronoUnit.MINUTES.between(start, end);
            assertSessionSpanWithinMaxMinutes(betweenMinutes);
            ZonedDateTime startZoned = start.atZone(zone);
            // Surf-day label uses start instant's local calendar date at the spot (not end); spans crossing midnight stay attributed to the day the session started.
            LocalDate sessionDate = startZoned.toLocalDate();
            return new ResolvedTiming(sessionDate, Math.toIntExact(betweenMinutes), start, end);
        }

        ZonedDateTime startZoned = start.atZone(zone);
        LocalDate sessionDate = startZoned.toLocalDate();
        return new ResolvedTiming(sessionDate, null, start, null);
    }

    private ResolvedTiming resolveFromManualLocalTimes(
            SurfSessionRequest request, SurfSpot surfSpot, LocalDate sessionDate) {
        LocalTime start = request.getSessionStartTime();
        LocalTime end = request.getSessionEndTime();

        boolean hasStart = start != null;
        boolean hasEnd = end != null;

        if (hasEnd && !hasStart) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_END_TIME_REQUIRES_START);
        }

        Instant startInstant = null;
        Instant endInstant = null;
        ZoneId zone = zoneForSpotDisplay(surfSpot);
        if (hasStart) {
            startInstant = ZonedDateTime.of(sessionDate, start, zone).toInstant();
        }
        if (hasEnd) {
            endInstant = ZonedDateTime.of(sessionDate, end, zone).toInstant();
        }

        if (hasStart && hasEnd) {
            if (!end.isAfter(start)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_END_BEFORE_START);
            }
            long between = ChronoUnit.MINUTES.between(start, end);
            assertSessionSpanWithinMaxMinutes(between);
            int computed = Math.toIntExact(between);
            return new ResolvedTiming(sessionDate, computed, startInstant, endInstant);
        }

        if (hasStart) {
            return new ResolvedTiming(sessionDate, null, startInstant, endInstant);
        }

        return new ResolvedTiming(sessionDate, null, null, null);
    }

    private static void assertSessionSpanWithinMaxMinutes(long minutesBetween) {
        if (minutesBetween > MAX_SESSION_DURATION_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_DURATION_MINUTES_INVALID);
        }
    }

    private Map<String, Long> countBy(List<SurfSession> sessions, Function<SurfSession, String> extractor) {
        return sessions.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    }
}
