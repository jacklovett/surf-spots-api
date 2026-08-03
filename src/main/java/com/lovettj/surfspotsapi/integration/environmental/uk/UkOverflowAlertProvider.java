package com.lovettj.surfspotsapi.integration.environmental.uk;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.lovettj.surfspotsapi.dto.EnvironmentalAlertCandidate;
import com.lovettj.surfspotsapi.integration.environmental.uk.ScottishWaterFeedProperties;
import com.lovettj.surfspotsapi.integration.environmental.uk.UkArcGisCompanyProperties;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertSeverity;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertType;
import com.lovettj.surfspotsapi.integration.environmental.EnvironmentalAlertProvider;
import com.lovettj.surfspotsapi.integration.environmental.GeoDistance;
import com.lovettj.surfspotsapi.integration.environmental.uk.ScottishWaterOverflowFeedClient.OutletStatus;

/**
 * Single UK environmental overflow provider. Company feed differences stay internal.
 * Country: united-kingdom. Feed routing: nation region slugs
 * (england / scotland / wales / northern-ireland).
 */
@Component
@ConditionalOnProperty(name = "app.environmental-alerts.enabled", havingValue = "true")
public class UkOverflowAlertProvider implements EnvironmentalAlertProvider {

    private static final String SLUG_SCOTLAND = "scotland";

    private final UkOverflowProperties ukConfig;
    private final ScottishWaterOverflowFeedClient scottishFeed;
    private final List<ArcGisOverflowFeed> arcGisFeeds;
    private final Set<String> supportedCountrySlugs;
    private final Set<String> nationRegionSlugs;

    public UkOverflowAlertProvider(
            UkOverflowProperties ukConfig,
            ScottishWaterOverflowFeedClient scottishFeed,
            RestClient environmentalAlertRestClient) {
        this.ukConfig = ukConfig;
        this.scottishFeed = scottishFeed;
        this.supportedCountrySlugs = toSlugSet(ukConfig.getCountrySlugs());
        this.nationRegionSlugs = toSlugSet(ukConfig.getNationRegionSlugs());
        List<ArcGisOverflowFeed> feeds = new ArrayList<>();
        for (UkArcGisCompanyProperties company : ukConfig.getStreamCompanies()) {
            if (company != null && company.isEnabled() && company.getQueryUrl() != null) {
                feeds.add(new ArcGisOverflowFeed(
                        company,
                        environmentalAlertRestClient,
                        ukConfig.getMatchRadiusMetres(),
                        ukConfig.getFeedCacheTtl()));
            }
        }
        this.arcGisFeeds = List.copyOf(feeds);
    }

    @Override
    public String getProviderKey() {
        return ukConfig.getProviderKey();
    }

    @Override
    public boolean supports(Country country) {
        if (!ukConfig.isEnabled() || country == null || country.getSlug() == null) {
            return false;
        }
        return supportedCountrySlugs.contains(country.getSlug().toLowerCase(Locale.ROOT));
    }

    @Override
    public List<EnvironmentalAlertCandidate> fetchAlerts(SurfSpot surfSpot) {
        String nationSlug = resolveNationRegionSlug(surfSpot);
        if (nationSlug == null) {
            return List.of();
        }

        if (SLUG_SCOTLAND.equals(nationSlug)) {
            return fetchScottish(surfSpot);
        }

        List<EnvironmentalAlertCandidate> candidates = new ArrayList<>();
        for (ArcGisOverflowFeed feed : arcGisFeeds) {
            if (nationSlug.equalsIgnoreCase(feed.nationRegionSlug())) {
                candidates.addAll(feed.fetchForSpot(surfSpot));
            }
        }
        return candidates;
    }

    /**
     * Prefer spot.region when it is a nation; otherwise use subRegion.region
     * (e.g. Cornwall under England).
     */
    private String resolveNationRegionSlug(SurfSpot surfSpot) {
        if (surfSpot == null) {
            return null;
        }
        String fromRegion = slugIfNation(surfSpot.getRegion());
        if (fromRegion != null) {
            return fromRegion;
        }
        SubRegion subRegion = surfSpot.getSubRegion();
        if (subRegion == null) {
            return null;
        }
        return slugIfNation(subRegion.getRegion());
    }

    private String slugIfNation(Region region) {
        if (region == null || region.getSlug() == null || region.getSlug().isBlank()) {
            return null;
        }
        String normalised = region.getSlug().toLowerCase(Locale.ROOT);
        return nationRegionSlugs.contains(normalised) ? normalised : null;
    }

    private List<EnvironmentalAlertCandidate> fetchScottish(SurfSpot surfSpot) {
        ScottishWaterFeedProperties config = ukConfig.getScottishWater();
        if (!config.isEnabled() || surfSpot.getLatitude() == null || surfSpot.getLongitude() == null) {
            return List.of();
        }
        List<OutletStatus> outlets = scottishFeed.fetchActiveOrRecentOutlets();
        if (outlets.isEmpty()) {
            return List.of();
        }

        double radiusMetres = ukConfig.getMatchRadiusMetres();
        List<EnvironmentalAlertCandidate> candidates = new ArrayList<>();
        for (OutletStatus outlet : outlets) {
            double distanceMetres = GeoDistance.metresBetween(
                    surfSpot.getLatitude(), surfSpot.getLongitude(), outlet.latitude(), outlet.longitude());
            if (distanceMetres <= radiusMetres) {
                candidates.add(toScottishCandidate(outlet, config));
            }
        }
        return candidates;
    }

    private static EnvironmentalAlertCandidate toScottishCandidate(
            OutletStatus outlet, ScottishWaterFeedProperties config) {
        boolean overflowing = outlet.statusId() == ScottishWaterOverflowFeedClient.STATUS_OVERFLOWING;
        EnvironmentalAlertSeverity severity = overflowing ? EnvironmentalAlertSeverity.WARNING : EnvironmentalAlertSeverity.CAUTION;
        Instant detectedAt = outlet.overflowStart() != null ? outlet.overflowStart() : Instant.now();
        Instant expiresAt = outlet.overflowEnd() != null
                ? outlet.overflowEnd().plus(48, ChronoUnit.HOURS)
                : Instant.now().plus(2, ChronoUnit.HOURS);

        String assetLabel = outlet.assetName() != null ? outlet.assetName() : outlet.assetId();
        String waterLabel =
                outlet.receivingWater() != null ? outlet.receivingWater() : "a nearby watercourse";
        String description = (overflowing ? "Active" : "Recent")
                + " sewage discharge at "
                + assetLabel
                + " into "
                + waterLabel
                + ". Think twice before paddling out - illness risk can stay elevated"
                + " for up to 48 hours after a discharge stops.";

        return new EnvironmentalAlertCandidate(
                EnvironmentalAlertType.SEWAGE_OVERFLOW,
                severity,
                "Sewage pollution alert",
                description,
                config.getSourceName(),
                config.getSourceUrl(),
                outlet.assetId(),
                detectedAt,
                expiresAt);
    }

    private static Set<String> toSlugSet(List<String> slugs) {
        Set<String> slugSet = new HashSet<>();
        if (slugs == null) {
            return slugSet;
        }
        for (String slug : slugs) {
            if (slug != null && !slug.isBlank()) {
                slugSet.add(slug.trim().toLowerCase(Locale.ROOT));
            }
        }
        return slugSet;
    }
}
