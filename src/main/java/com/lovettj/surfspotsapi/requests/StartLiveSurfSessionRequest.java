package com.lovettj.surfspotsapi.requests;

import com.lovettj.surfspotsapi.response.ApiErrors;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.Instant;

@Data
public class StartLiveSurfSessionRequest {

    @Min(value = -90, message = ApiErrors.COORDINATE_LATITUDE_OUT_OF_RANGE)
    @Max(value = 90, message = ApiErrors.COORDINATE_LATITUDE_OUT_OF_RANGE)
    private Double startLatitude;

    @Min(value = -180, message = ApiErrors.COORDINATE_LONGITUDE_OUT_OF_RANGE)
    @Max(value = 180, message = ApiErrors.COORDINATE_LONGITUDE_OUT_OF_RANGE)
    private Double startLongitude;

    private boolean shareLocationWithEmergencyContact;

    private Instant expectedReturnInstant;

    private String surfboardId;

    /** IANA time zone from the device at start (e.g. Europe/Dublin). */
    private String startIanaZoneId;

    @AssertTrue(message = "Provide both start latitude and start longitude, or omit both.")
    public boolean isStartCoordinatesPairComplete() {
        boolean hasLatitude = startLatitude != null;
        boolean hasLongitude = startLongitude != null;
        return hasLatitude == hasLongitude;
    }

    @AssertTrue(message = ApiErrors.LIVE_SESSION_START_COORDINATES_REQUIRED)
    public boolean isStartCoordinatesProvided() {
        return startLatitude != null && startLongitude != null;
    }

    @AssertTrue(message = ApiErrors.LIVE_SESSION_EXPECTED_RETURN_REQUIRED)
    public boolean isExpectedReturnProvidedWhenSharing() {
        if (!shareLocationWithEmergencyContact) {
            return true;
        }
        return expectedReturnInstant != null;
    }
}
