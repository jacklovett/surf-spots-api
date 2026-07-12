package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.response.ApiErrors;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkSessionsToSpotRequest {

    @NotNull(message = ApiErrors.LINK_SESSIONS_SURF_SPOT_ID_REQUIRED)
    private Long surfSpotId;

    /** GPS anchor for matching spotless live sessions (typically the session start location). */
    @NotNull(message = ApiErrors.LINK_SESSIONS_ANCHOR_LATITUDE_REQUIRED)
    @Min(value = -90, message = ApiErrors.COORDINATE_LATITUDE_OUT_OF_RANGE)
    @Max(value = 90, message = ApiErrors.COORDINATE_LATITUDE_OUT_OF_RANGE)
    private Double anchorLatitude;

    @NotNull(message = ApiErrors.LINK_SESSIONS_ANCHOR_LONGITUDE_REQUIRED)
    @Min(value = -180, message = ApiErrors.COORDINATE_LONGITUDE_OUT_OF_RANGE)
    @Max(value = 180, message = ApiErrors.COORDINATE_LONGITUDE_OUT_OF_RANGE)
    private Double anchorLongitude;

    /** When set, this session is linked even if it falls outside the match radius. */
    private Long sessionId;
}
