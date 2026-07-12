package com.lovettj.surfspotsapi.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LinkSessionsToSpotResultDTO {
    private int linkedSessionCount;
}
