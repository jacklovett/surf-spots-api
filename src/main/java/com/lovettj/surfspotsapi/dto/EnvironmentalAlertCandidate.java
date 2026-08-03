package com.lovettj.surfspotsapi.dto;

import java.time.Instant;

import com.lovettj.surfspotsapi.enums.EnvironmentalAlertSeverity;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertType;

/** Provider output before persistence. Does not include surf spot or status. */
public record EnvironmentalAlertCandidate(
        EnvironmentalAlertType type,
        EnvironmentalAlertSeverity severity,
        String title,
        String description,
        String sourceName,
        String sourceUrl,
        String externalId,
        Instant detectedAt,
        Instant expiresAt) {}
