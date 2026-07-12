package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveFace;
import com.lovettj.surfspotsapi.enums.WaveSize;
import com.lovettj.surfspotsapi.response.ApiErrors;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EndLiveSurfSessionRequest {

    private WaveSize waveSize;

    private CrowdLevel crowdLevel;

    private WaveFace waveFace;

    @Min(value = 1, message = ApiErrors.SESSION_RATING_OUT_OF_RANGE)
    @Max(value = 5, message = ApiErrors.SESSION_RATING_OUT_OF_RANGE)
    private Integer sessionRating;

    private Tide tide;

    private String swellDirection;

    private String windDirection;

    @Size(max = 2000, message = "Session notes must be at most 2000 characters")
    private String sessionNotes;

    private SkillLevel skillLevel;

    private String surfboardId;

    /** Optional: attach or correct surf spot when ending a live session. */
    private Long surfSpotId;
}
