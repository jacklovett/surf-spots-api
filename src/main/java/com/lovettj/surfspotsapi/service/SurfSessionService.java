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
import java.util.ArrayList;
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

        Surfboard surfboard = loadOptionalSurfboardForSessionRequest(request, request.getUserId());
        ResolvedTiming timing = resolveTiming(request, surfSpot);

        ExternalSyncPair externalSync = resolveExternalSyncPair(request);
        if (externalSync != null
                && surfSessionRepository.externalSessionAlreadyRecordedForUser(
                        request.getUserId(), externalSync.provider(), externalSync.externalId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ApiErrors.SURF_SESSION_ALREADY_SYNCED);
        }

        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .externalSessionProvider(externalSync != null ? externalSync.provider() : null)
                .externalSessionId(externalSync != null ? externalSync.externalId() : null)
                .build();

        applyTimingSkillAndEditableSessionFieldsFromRequest(
                session, request, timing, userSkillLevel, surfboard);

        persistSessionOrConflictOnDuplicateExternalId(session, externalSync != null);

        // Idempotent: ensures the spot appears in surfed spots without a separate "I surfed here" step.
        userSurfSpotService.addUserSurfSpot(request.getUserId(), request.getSurfSpotId());
    }

    /**
     * Loads one session for the signed-in user (sessions list row shape, including signed media URLs when configured).
     */
    public SurfSessionListItemDTO getSessionByIdForUser(String userId, Long sessionId) {
        SurfSession session = surfSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.SURF_SESSION_NOT_FOUND));
        
        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.SURF_SESSION_ACCESS_FORBIDDEN);
        }
        
        SurfSessionListItemDTO dto = toListItem(session);
        applySignedMediaUrls(dto);
        return dto;
    }

    /**
     * Updates fields the user can edit in the app. External sync keys ({@link SurfSession#getExternalSessionProvider()},
     * {@link SurfSession#getExternalSessionId()}) are never changed here, so partner records are not modified.
     */
    @Transactional
    public void updateSession(String userId, Long sessionId, SurfSessionRequest request) {
        SurfSession session = surfSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.SURF_SESSION_NOT_FOUND));
        
        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.SURF_SESSION_ACCESS_FORBIDDEN);
        }
        
        if (request.getSurfSpotId() == null
                || !request.getSurfSpotId().equals(session.getSurfSpot().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SURF_SESSION_SPOT_MISMATCH);
        }

        User user = session.getUser();
        // Preserve the session's stored skill unless the client sends an explicit skill (first-time profile fill).
        SkillLevel skillForSession = request.getSkillLevel() != null
                ? request.getSkillLevel()
                : (session.getSkillLevel() != null
                        ? session.getSkillLevel()
                        : user.getSkillLevel());
        if (skillForSession == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SKILL_LEVEL_REQUIRED_FOR_SESSION);
        }
        if (user.getSkillLevel() == null && request.getSkillLevel() != null) {
            user.setSkillLevel(request.getSkillLevel());
            userRepository.save(user);
        }

        SurfSpot surfSpot = session.getSurfSpot();
        ResolvedTiming timing = resolveTimingForUpdate(session, request, surfSpot);

        Surfboard surfboard = loadOptionalSurfboardForSessionRequest(request, userId);
        applyTimingSkillAndEditableSessionFieldsFromRequest(
                session, request, timing, skillForSession, surfboard);

        surfSessionRepository.save(session);
    }

    /**
     * Shared by {@link #createSession} and {@link #updateSession}: timing, skill on the row, and user-editable
     * session content from the request (not external sync ids).
     */
    private void applyTimingSkillAndEditableSessionFieldsFromRequest(
            SurfSession session,
            SurfSessionRequest request,
            ResolvedTiming timing,
            SkillLevel skillLevel,
            Surfboard surfboard) {
        session.setSkillLevel(skillLevel);
        session.setSessionDate(timing.sessionDate());
        session.setDurationMinutes(timing.durationMinutes());
        session.setSessionStartInstant(timing.sessionStartInstant());
        session.setSessionEndInstant(timing.sessionEndInstant());
        session.setWaveSize(request.getWaveSize());
        session.setCrowdLevel(request.getCrowdLevel());
        session.setWaveFace(request.getWaveFace());
        session.setSessionRating(request.getSessionRating());
        session.setSwellDirection(blankToNull(request.getSwellDirection()));
        session.setWindDirection(blankToNull(request.getWindDirection()));
        session.setTide(request.getTide());
        session.setSessionNotes(blankToNull(request.getSessionNotes()));
        session.setSurfboard(surfboard);
    }

    private Surfboard loadOptionalSurfboardForSessionRequest(SurfSessionRequest request, String userId) {
        if (request.getSurfboardId() == null || request.getSurfboardId().isBlank()) {
            return null;
        }
        return surfboardRepository
                .findByIdAndUserId(request.getSurfboardId().trim(), userId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SURFBOARD_NOT_FOUND_FOR_USER));
    }

    /**
     * Deletes the session row (cascade removes media rows). Object storage is cleared first per file; if the DB
     * transaction fails after a successful storage delete, orphaned objects may remain (same tradeoff as
     * {@link #deleteMedia}).
     */
    @Transactional
    public void deleteSession(String userId, Long sessionId) {
        SurfSession session = surfSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.SURF_SESSION_NOT_FOUND));
        if (!session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ApiErrors.SURF_SESSION_ACCESS_FORBIDDEN);
        }
        List<SurfSessionMedia> mediaList = session.getMedia();
        if (mediaList != null && !mediaList.isEmpty()) {
            for (SurfSessionMedia media : new ArrayList<>(mediaList)) {
                deleteSessionMediaObjectFromStorage(media);
            }
        }
        surfSessionRepository.delete(session);
    }

    private void deleteSessionMediaObjectFromStorage(SurfSessionMedia media) {
        String mediaType = media.getMediaType() != null ? media.getMediaType() : "image";
        String objectKey = storageService.resolveObjectKeyWithFallback(
                media.getObjectKey(),
                media.getOriginalUrl(),
                media.getId(),
                mediaType,
                "surf-sessions/media");
        storageService.deleteObject(objectKey);
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
                .waveFace(s.getWaveFace())
                .sessionRating(s.getSessionRating())
                .swellDirection(s.getSwellDirection())
                .windDirection(s.getWindDirection())
                .tide(s.getTide())
                .sessionNotes(s.getSessionNotes())
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
        deleteSessionMediaObjectFromStorage(media);
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

        Map<String, Long> crowdDistribution =
                countBy(sessions, s -> s.getCrowdLevel() != null ? s.getCrowdLevel().name() : null);
        Map<String, Long> sessionRatingDistribution = countBy(
                sessions,
                s -> s.getSessionRating() != null ? String.valueOf(s.getSessionRating()) : null);

        return SurfSessionSummaryDTO.builder()
                .skillLevel(effectiveSkillLevel)
                .sampleSize(sessions.size())
                .waveSizeDistribution(countBy(sessions, s -> s.getWaveSize() != null ? s.getWaveSize().name() : null))
                .crowdDistribution(crowdDistribution)
                .sessionRatingDistribution(sessionRatingDistribution)
                .fallbackToAllSkills(fallbackToAllSkills)
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * When the session was stored with UTC instants (imports / wearables) and the client omits all
     * timing fields, keep the stored timeline so a notes-only update does not clear instants via the
     * manual-date path in {@link #resolveTiming}.
     */
    private ResolvedTiming resolveTimingForUpdate(
            SurfSession session, SurfSessionRequest request, SurfSpot surfSpot) {
        boolean sessionStoredInstants =
                session.getSessionStartInstant() != null || session.getSessionEndInstant() != null;
        if (sessionStoredInstants && !requestSpecifiesExplicitTiming(request)) {
            return new ResolvedTiming(
                    session.getSessionDate(),
                    session.getDurationMinutes(),
                    session.getSessionStartInstant(),
                    session.getSessionEndInstant());
        }
        return resolveTiming(request, surfSpot);
    }

    private static boolean requestSpecifiesExplicitTiming(SurfSessionRequest request) {
        return request.getSessionStartInstant() != null
                || request.getSessionEndInstant() != null
                || request.getSessionStartTime() != null
                || request.getSessionEndTime() != null;
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
