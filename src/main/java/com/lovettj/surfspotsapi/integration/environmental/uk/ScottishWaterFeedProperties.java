package com.lovettj.surfspotsapi.integration.environmental.uk;

/**
 * Scottish Water REST feed settings under {@code app.environmental-alerts.uk.scottish-water}.
 * URLs and source labels are set in {@code application.yml}.
 * Match radius and cache TTL come from {@link UkOverflowProperties}.
 */
public class ScottishWaterFeedProperties {

    private boolean enabled = true;
    private String apiUrl;
    private String sourceName;
    private String sourceUrl;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabledValue) {
        this.enabled = enabledValue;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrlValue) {
        this.apiUrl = apiUrlValue;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceNameValue) {
        this.sourceName = sourceNameValue;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrlValue) {
        this.sourceUrl = sourceUrlValue;
    }
}
