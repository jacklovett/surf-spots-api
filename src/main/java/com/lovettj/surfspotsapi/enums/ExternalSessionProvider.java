package com.lovettj.surfspotsapi.enums;

/**
 * Integration source for idempotent external session imports. Persisted as the constant name in {@code external_session_provider}.
 */
public enum ExternalSessionProvider {
    SURFLINE,
    GARMIN,
    /** Rip Curl Search GPS3 watch / companion app. */
    RIP_CURL_SEARCH_GPS3
}
