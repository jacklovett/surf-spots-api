package com.lovettj.surfspotsapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code application.yml} under {@code app.*}. Defaults live only in YAML
 * (e.g. {@code app.url: ${APP_URL:...}}), not duplicated in {@code @Value} annotations.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Public web app origin (links in email, redirects). No trailing slash.
     * Environment: {@code APP_URL} → {@code app.url}.
     */
    private String url;

    /**
     * Browser-reachable API base for email action links (e.g. verify-email). No trailing slash.
     * Environment: {@code APP_PUBLIC_API_URL} → {@code app.public-api-base-url}.
     */
    private String publicApiBaseUrl;

    /**
     * Mapbox access token used server-side to generate static map image URLs for session
     * notification emails. Same token as {@code VITE_MAP_ACCESS_TOKEN} in the frontend.
     * Environment: {@code MAPBOX_ACCESS_TOKEN} → {@code app.mapbox.access-token}.
     */
    private Mapbox mapbox = new Mapbox();

    public String getUrl() {
        return url;
    }

    public void setUrl(String urlValue) {
        this.url = urlValue;
    }

    public String getPublicApiBaseUrl() {
        return publicApiBaseUrl;
    }

    public void setPublicApiBaseUrl(String publicApiBaseUrlValue) {
        this.publicApiBaseUrl = publicApiBaseUrlValue;
    }

    public Mapbox getMapbox() {
        return mapbox;
    }

    public void setMapbox(Mapbox mapboxValue) {
        this.mapbox = mapboxValue;
    }

    public static class Mapbox {
        private String accessToken;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessTokenValue) {
            this.accessToken = accessTokenValue;
        }
    }
}
