package com.lovettj.surfspotsapi.integration.environmental.uk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code app.environmental-alerts.uk.*} in {@code application.yml}.
 * Company feed URLs and display names live in YAML only.
 * Match radius and feed cache TTL are UK-wide (one place to change).
 */
@ConfigurationProperties(prefix = "app.environmental-alerts.uk")
public class UkOverflowProperties {

    private boolean enabled = true;
    private String providerKey = "uk-overflow";
    private List<String> countrySlugs = new ArrayList<>(List.of("united-kingdom"));
    private List<String> nationRegionSlugs =
            new ArrayList<>(List.of("england", "scotland", "wales", "northern-ireland"));
    /** Shared by Scottish Water and all ArcGIS company feeds. */
    private double matchRadiusMetres = 3000;
    /** Shared by Scottish Water and all ArcGIS company feeds. */
    private Duration feedCacheTtl = Duration.ofMinutes(5);
    private final ScottishWaterFeedProperties scottishWater = new ScottishWaterFeedProperties();
    private List<UkArcGisCompanyProperties> streamCompanies = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabledValue) {
        this.enabled = enabledValue;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKeyValue) {
        this.providerKey = providerKeyValue;
    }

    public List<String> getCountrySlugs() {
        return countrySlugs;
    }

    public void setCountrySlugs(List<String> countrySlugsValue) {
        this.countrySlugs = countrySlugsValue != null ? countrySlugsValue : new ArrayList<>();
    }

    public List<String> getNationRegionSlugs() {
        return nationRegionSlugs;
    }

    public void setNationRegionSlugs(List<String> nationRegionSlugsValue) {
        this.nationRegionSlugs =
                nationRegionSlugsValue != null ? nationRegionSlugsValue : new ArrayList<>();
    }

    public double getMatchRadiusMetres() {
        return matchRadiusMetres;
    }

    public void setMatchRadiusMetres(double matchRadiusMetresValue) {
        this.matchRadiusMetres = matchRadiusMetresValue;
    }

    public Duration getFeedCacheTtl() {
        return feedCacheTtl;
    }

    public void setFeedCacheTtl(Duration feedCacheTtlValue) {
        this.feedCacheTtl = feedCacheTtlValue;
    }

    public ScottishWaterFeedProperties getScottishWater() {
        return scottishWater;
    }

    public List<UkArcGisCompanyProperties> getStreamCompanies() {
        return streamCompanies;
    }

    public void setStreamCompanies(List<UkArcGisCompanyProperties> streamCompaniesValue) {
        this.streamCompanies =
                streamCompaniesValue != null ? streamCompaniesValue : new ArrayList<>();
    }
}
