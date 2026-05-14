package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.ExternalSessionProvider;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.response.ApiErrors;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SurfSessionRequest {
    @NotNull(message = "Surf spot id is required")
    private Long surfSpotId;

    /**
     * Calendar day for manual logging (with optional {@link #sessionStartTime} / {@link #sessionEndTime}).
     * Omit when {@link #sessionStartInstant} is provided (wearables/partners); the server derives the date from instants.
     */
    private LocalDate sessionDate;

    /**
     * Optional local times on {@link #sessionDate} (same calendar day). When both are set,
     * duration is derived server-side and stored; end requires start.
     * Ignored when {@link #sessionStartInstant} is set (instant-based timing wins if both are sent).
     */
    private LocalTime sessionStartTime;

    private LocalTime sessionEndTime;

    /**
     * Wearable or partner-supplied start (ISO-8601 instant). When set, drives stored UTC instants and derived local fields.
     * Takes precedence over {@link #sessionDate} / {@link #sessionStartTime} / {@link #sessionEndTime} when those are also present.
     */
    private Instant sessionStartInstant;

    private Instant sessionEndInstant;

    private WaveSize waveSize;

    private CrowdLevel crowdLevel;

    private WaveQuality waveQuality;

    private Tide tide;

    private String swellDirection;

    private String windDirection;

    /**
     * User-authored session notes. Do not send partner-only structured data here; add a dedicated model when required.
     */
    @Size(max = 2000, message = "Session notes must be at most 2000 characters")
    private String sessionNotes;

    private Boolean wouldSurfAgain;

    private String userId;

    private SkillLevel skillLevel;

    private String surfboardId;

    /**
     * Integration source for {@link #externalSessionId}. Omit together with id for UI-only logs.
     */
    private ExternalSessionProvider externalSessionProvider;

    /**
     * Provider-local stable id for idempotent imports. Unique per user together with {@link #externalSessionProvider}.
     */
    @Size(max = 255, message = "External session id must be at most 255 characters")
    private String externalSessionId;

    @AssertTrue(message = ApiErrors.EXTERNAL_SESSION_SYNC_PAIR_REQUIRED)
    public boolean isExternalSyncPairComplete() {
        boolean hasId = externalSessionId != null && !externalSessionId.trim().isEmpty();
        boolean hasProvider = externalSessionProvider != null;
        return (hasId && hasProvider) || (!hasId && !hasProvider);
    }

    @AssertTrue(message = "Session date is required unless session start instant is provided.")
    public boolean isSessionDateOrStartInstantProvided() {
        return sessionDate != null || sessionStartInstant != null;
    }
}
