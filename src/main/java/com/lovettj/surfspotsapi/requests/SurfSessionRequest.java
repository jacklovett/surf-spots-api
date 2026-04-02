package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SurfSessionRequest {
    @NotNull(message = "Surf spot id is required")
    private Long surfSpotId;

    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;

    private WaveSize waveSize;

    private CrowdLevel crowdLevel;

    private WaveQuality waveQuality;

    private Tide tide;

    private String swellDirection;

    private String windDirection;

    @Size(max = 2000, message = "Session notes must be at most 2000 characters")
    private String sessionNotes;

    private Boolean wouldSurfAgain;

    @NotNull(message = "User id is required")
    private String userId;

    private SkillLevel skillLevel;

    private String surfboardId;
}
