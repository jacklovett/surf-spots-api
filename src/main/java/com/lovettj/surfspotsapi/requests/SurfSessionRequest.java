package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SurfSessionRequest {
    @NotNull(message = "Surf spot id is required")
    private Long surfSpotId;

    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;

    @NotNull(message = "Wave size is required")
    private WaveSize waveSize;

    @NotNull(message = "Crowd level is required")
    private CrowdLevel crowdLevel;

    @NotNull(message = "Wave quality is required")
    private WaveQuality waveQuality;

    @NotNull(message = "Would surf again is required")
    private Boolean wouldSurfAgain;

    @NotNull(message = "User id is required")
    private String userId;

    private SkillLevel skillLevel;

    private String surfboardId;
}
