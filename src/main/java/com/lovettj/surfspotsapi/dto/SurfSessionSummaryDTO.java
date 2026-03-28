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
    private Map<String, Long> waveQualityDistribution;
    /** Count of sessions where {@code wouldSurfAgain} was true vs false (each session stores a boolean). */
    private long wouldSurfAgainTrueCount;
    private long wouldSurfAgainFalseCount;
    private boolean fallbackToAllSkills;

    /** UI line, e.g. "Intermediate (6 sessions)" or "Surfers (6 sessions)". */
    private String segmentHeadline;
    /** Human-readable trend from wave quality distribution; null if unknown. */
    private String waveQualityTrendLine;
    /** Human-readable trend from crowd distribution; null if unknown. */
    private String crowdTrendLine;
    /** e.g. "4/6 would surf again"; null if no responses. */
    private String wouldSurfAgainLine;
}
