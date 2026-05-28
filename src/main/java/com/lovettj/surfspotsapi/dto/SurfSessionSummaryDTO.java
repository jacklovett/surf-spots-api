package com.lovettj.surfspotsapi.dto;

import com.lovettj.surfspotsapi.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SurfSessionSummaryDTO {
    private SkillLevel skillLevel;
    private int sampleSize;
    private Map<String, Long> waveSizeDistribution;
    private Map<String, Long> crowdDistribution;
    /** Counts keyed by rating string "1".."5". */
    private Map<String, Long> sessionRatingDistribution;
    private boolean fallbackToAllSkills;
}
