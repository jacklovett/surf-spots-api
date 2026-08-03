package com.lovettj.surfspotsapi.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cross-cutting settings from {@code app.environmental-alerts.*}.
 * Provider-specific blocks (URLs, country slugs, company feeds) live on each
 * provider's own {@code @ConfigurationProperties} class next to that provider.
 */
@ConfigurationProperties(prefix = "app.environmental-alerts")
public class EnvironmentalAlertsProperties {

    private boolean enabled = false;
    private String syncCron = "0 */30 * * * *";
    private final Http http = new Http();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabledValue) {
        this.enabled = enabledValue;
    }

    public String getSyncCron() {
        return syncCron;
    }

    public void setSyncCron(String syncCronValue) {
        this.syncCron = syncCronValue;
    }

    public Http getHttp() {
        return http;
    }

    public static class Http {
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(60);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeoutValue) {
            this.connectTimeout = connectTimeoutValue;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeoutValue) {
            this.readTimeout = readTimeoutValue;
        }
    }
}
