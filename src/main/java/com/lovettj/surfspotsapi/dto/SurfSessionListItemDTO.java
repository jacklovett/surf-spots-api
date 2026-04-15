package com.lovettj.surfspotsapi.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.lovettj.surfspotsapi.enums.CrowdLevel;
import com.lovettj.surfspotsapi.enums.SkillLevel;
import com.lovettj.surfspotsapi.enums.Tide;
import com.lovettj.surfspotsapi.enums.WaveQuality;
import com.lovettj.surfspotsapi.enums.WaveSize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfSessionListItemDTO {
    private Long id;
    private LocalDate sessionDate;
    private LocalDateTime createdAt;
    private Long surfSpotId;
    private String surfSpotName;
    /** Client route path (e.g. /surf-spots/europe/.../spot-slug). */
    private String spotPath;
    private WaveSize waveSize;
    private CrowdLevel crowdLevel;
    private WaveQuality waveQuality;
    private String swellDirection;
    private String windDirection;
    private Tide tide;
    private String sessionNotes;
    private Boolean wouldSurfAgain;
    private SkillLevel skillLevel;
    private String surfboardId;
    private String surfboardName;
    private List<SurfSessionMediaDTO> media;
}
