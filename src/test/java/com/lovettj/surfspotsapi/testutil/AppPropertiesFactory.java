package com.lovettj.surfspotsapi.testutil;

import com.lovettj.surfspotsapi.config.AppProperties;

/**
 * Test fixtures for {@link AppProperties} (defaults align with {@code application.yml} local dev).
 */
public final class AppPropertiesFactory {

    /** Same defaults as local {@code application.yml} (used by tests and {@link #localhostDefaults()}). */
    public static final String LOCAL_APP_URL = "http://localhost:5173";

    public static final String LOCAL_PUBLIC_API_BASE_URL = "http://localhost:8080";

    private AppPropertiesFactory() {
    }

    public static AppProperties localhostDefaults() {
        return withUrls(LOCAL_APP_URL, LOCAL_PUBLIC_API_BASE_URL);
    }

    public static AppProperties withUrls(String appUrl, String publicApiBaseUrl) {
        AppProperties properties = new AppProperties();
        properties.setUrl(appUrl);
        properties.setPublicApiBaseUrl(publicApiBaseUrl);
        return properties;
    }
}
