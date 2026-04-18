package com.lovettj.surfspotsapi.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A user's surf sessions for the sessions page: headline stats plus the session list (same idea as
 * {@link UserSurfSpotsDTO} and {@link com.lovettj.surfspotsapi.dto.WatchListDTO}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSurfSessionsDTO {
    private long totalSessions;
    private long spotsSurfedCount;
    private long boardsUsedCount;
    private List<SurfSessionListItemDTO> sessions;
}
