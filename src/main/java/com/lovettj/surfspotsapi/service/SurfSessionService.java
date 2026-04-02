package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.dto.SurfSessionListItemDTO;
import com.lovettj.surfspotsapi.dto.SurfSessionSummaryDTO;
import com.lovettj.surfspotsapi.entity.SurfSession;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.entity.Surfboard;
import com.lovettj.surfspotsapi.entity.User;
import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.repository.SurfSessionRepository;
import com.lovettj.surfspotsapi.util.SurfSpotPathUtil;
import com.lovettj.surfspotsapi.repository.SurfSpotRepository;
import com.lovettj.surfspotsapi.repository.SurfboardRepository;
import com.lovettj.surfspotsapi.repository.UserRepository;
import com.lovettj.surfspotsapi.requests.SurfSessionRequest;
import com.lovettj.surfspotsapi.response.ApiErrors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SurfSessionService {
    private static final int MIN_SAMPLE_FOR_SKILL_SEGMENT = 3;

    private final SurfSessionRepository surfSessionRepository;
    private final SurfSpotRepository surfSpotRepository;
    private final UserRepository userRepository;
    private final SurfboardRepository surfboardRepository;
    private final UserSurfSpotService userSurfSpotService;

    public SurfSessionService(
            SurfSessionRepository surfSessionRepository,
            SurfSpotRepository surfSpotRepository,
            UserRepository userRepository,
            SurfboardRepository surfboardRepository,
            UserSurfSpotService userSurfSpotService) {
        this.surfSessionRepository = surfSessionRepository;
        this.surfSpotRepository = surfSpotRepository;
        this.userRepository = userRepository;
        this.surfboardRepository = surfboardRepository;
        this.userSurfSpotService = userSurfSpotService;
    }

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

        SurfSession session = SurfSession.builder()
                .user(user)
                .surfSpot(surfSpot)
                .skillLevel(userSkillLevel)
                .sessionDate(request.getSessionDate())
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

        surfSessionRepository.save(session);
        // Idempotent: ensures the spot appears in surfed spots without a separate "I surfed here" step.
        userSurfSpotService.addUserSurfSpot(request.getUserId(), surfSpot.getId());
    }

    /**
     * Logged sessions for the user, newest first (by session date, then creation time).
     */
    public List<SurfSessionListItemDTO> listSessionsForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApiErrors.SESSION_SUMMARY_USER_ID_REQUIRED);
        }
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ApiErrors.USER_NOT_FOUND);
        }
        return surfSessionRepository.findAllForUserList(userId).stream()
                .map(this::toListItem)
                .toList();
    }

    private SurfSessionListItemDTO toListItem(SurfSession s) {
        SurfSpot sp = s.getSurfSpot();
        String spotPath = SurfSpotPathUtil.pathFor(sp);
        Surfboard board = s.getSurfboard();
        return SurfSessionListItemDTO.builder()
                .id(s.getId())
                .sessionDate(s.getSessionDate())
                .createdAt(s.getCreatedAt())
                .surfSpotId(sp.getId())
                .surfSpotName(sp.getName())
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
                .build();
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

    private Map<String, Long> countBy(List<SurfSession> sessions, Function<SurfSession, String> extractor) {
        return sessions.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
    }
}
