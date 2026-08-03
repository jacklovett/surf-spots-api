package com.lovettj.surfspotsapi.integration.environmental.uk;

/**
 * One ArcGIS overflow company under {@code app.environmental-alerts.uk.stream-companies}.
 * URLs and source labels are set in {@code application.yml}.
 * Match radius and cache TTL come from {@link UkOverflowProperties}.
 */
public class UkArcGisCompanyProperties {

    private boolean enabled = true;
    private String id;
    private String queryUrl;
    private String sourceName;
    private String sourceUrl;
    /**
     * Feed dialect for this company layer (English API text, not UI language):
     * {@code southwest}, {@code southern}, or {@code welsh-water}.
     */
    private String fieldStyle = "southwest";
    /** Nation region slug this feed serves (e.g. {@code england}, {@code wales}). */
    private String nationRegionSlug = "england";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabledValue) {
        this.enabled = enabledValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String idValue) {
        this.id = idValue;
    }

    public String getQueryUrl() {
        return queryUrl;
    }

    public void setQueryUrl(String queryUrlValue) {
        this.queryUrl = queryUrlValue;
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

    public String getFieldStyle() {
        return fieldStyle;
    }

    public void setFieldStyle(String fieldStyleValue) {
        this.fieldStyle = fieldStyleValue;
    }

    public String getNationRegionSlug() {
        return nationRegionSlug;
    }

    public void setNationRegionSlug(String nationRegionSlugValue) {
        this.nationRegionSlug = nationRegionSlugValue;
    }
}
