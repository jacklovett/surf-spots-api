package com.lovettj.surfspotsapi.integration.environmental.uk;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.lovettj.surfspotsapi.integration.environmental.uk.UkArcGisCompanyProperties;
import com.lovettj.surfspotsapi.dto.EnvironmentalAlertCandidate;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertSeverity;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertType;
import com.lovettj.surfspotsapi.integration.environmental.GeoDistance;

/**
 * Shared ArcGIS overflow client for UK water-company layers. Not a Spring provider.
 *
 * <p>Two dialects (English text from each company API - not a language setting):
 * <ul>
 *   <li>{@code southwest} / {@code southern} (England Stream-style layers): numeric
 *       {@code status=1} means discharging, lat/lon are attributes, times are epoch millis.
 *       Always {@link EnvironmentalAlertSeverity#WARNING}.</li>
 *   <li>{@code welsh-water} (Welsh Water company layer): English status phrases, coordinates
 *       from Web Mercator {@code geometry}, times are Europe/London local date-times.
 *       Operating = {@link EnvironmentalAlertSeverity#WARNING}; recent =
 *       {@link EnvironmentalAlertSeverity#CAUTION}.</li>
 * </ul>
 */
final class ArcGisOverflowFeed {

    private static final Logger logger = LoggerFactory.getLogger(ArcGisOverflowFeed.class);
    private static final int STATUS_DISCHARGING = 1;
    private static final String WELSH_WATER_STATUS_OPERATING = "Overflow Operating";
    private static final String WELSH_WATER_STATUS_RECENT =
            "Overflow Not Operating (Has in the last 24 hours)";
    private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");

    private final UkArcGisCompanyProperties config;
    private final RestClient restClient;
    private final boolean welshWaterCompany;
    private final FieldNames fieldNames;
    private final double matchRadiusMetres;
    private final Duration feedCacheTtl;
    private final AtomicReference<CachedFeed> cache = new AtomicReference<>();

    ArcGisOverflowFeed(
            UkArcGisCompanyProperties config,
            RestClient restClient,
            double matchRadiusMetres,
            Duration feedCacheTtl) {
        this.config = config;
        this.restClient = restClient;
        this.welshWaterCompany = "welsh-water".equalsIgnoreCase(config.getFieldStyle());
        this.fieldNames = FieldNames.forStyle(config.getFieldStyle());
        this.matchRadiusMetres = matchRadiusMetres;
        this.feedCacheTtl = feedCacheTtl;
    }

    String nationRegionSlug() {
        return config.getNationRegionSlug();
    }

    List<EnvironmentalAlertCandidate> fetchForSpot(SurfSpot surfSpot) {
        if (surfSpot == null
                || !config.isEnabled()
                || surfSpot.getLatitude() == null
                || surfSpot.getLongitude() == null) {
            return List.of();
        }
        List<OutletStatus> outlets = fetchActiveOutlets();
        if (outlets.isEmpty()) {
            return List.of();
        }

        double radiusMetres = matchRadiusMetres;
        List<EnvironmentalAlertCandidate> candidates = new ArrayList<>();
        for (OutletStatus outlet : outlets) {
            double distanceMetres = GeoDistance.metresBetween(
                    surfSpot.getLatitude(), surfSpot.getLongitude(), outlet.latitude(), outlet.longitude());
            if (distanceMetres <= radiusMetres) {
                candidates.add(toCandidate(outlet));
            }
        }
        return candidates;
    }

    private List<OutletStatus> fetchActiveOutlets() {
        CachedFeed cached = cache.get();
        Instant now = Instant.now();
        Duration ttl = feedCacheTtl;
        if (cached != null && cached.fetchedAt().plus(ttl).isAfter(now)) {
            return cached.outlets();
        }

        UriComponentsBuilder queryBuilder = UriComponentsBuilder.fromUriString(config.getQueryUrl())
                .queryParam("where", buildWhereClause())
                .queryParam("outFields", "*")
                .queryParam("f", "json");
        if (welshWaterCompany) {
            queryBuilder.queryParam("returnGeometry", "true");
        }
        String queryUrl = queryBuilder.encode().build().toUriString();

        Map<String, Object> payload = restClient
                .get()
                .uri(queryUrl)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        List<OutletStatus> outlets =
                welshWaterCompany ? parseWelshWaterFeatures(payload) : parseNumericFeatures(payload);
        cache.set(new CachedFeed(now, outlets));
        logger.info(
                "{} overflow feed refreshed active={}",
                config.getSourceName(),
                outlets.size());
        return outlets;
    }

    private String buildWhereClause() {
        if (welshWaterCompany) {
            return fieldNames.status() + "='" + WELSH_WATER_STATUS_OPERATING + "' OR "
                    + fieldNames.status() + "='" + WELSH_WATER_STATUS_RECENT + "'";
        }
        return fieldNames.status() + "=1";
    }

    @SuppressWarnings("unchecked")
    private List<OutletStatus> parseNumericFeatures(Map<String, Object> payload) {
        if (payload == null || !(payload.get("features") instanceof List<?> features)) {
            return List.of();
        }
        List<OutletStatus> outlets = new ArrayList<>();
        for (Object featureObject : features) {
            if (!(featureObject instanceof Map<?, ?> feature)) {
                continue;
            }
            Object attributesNode = feature.get("attributes");
            if (!(attributesNode instanceof Map<?, ?> attributesRaw)) {
                continue;
            }
            Map<String, Object> attributes = (Map<String, Object>) attributesRaw;
            int status = parseInt(attributes.get(fieldNames.status()), -999);
            if (status != STATUS_DISCHARGING) {
                continue;
            }
            String id = resolveId(attributes);
            Double latitude = parseDouble(attributes.get(fieldNames.latitude()));
            Double longitude = parseDouble(attributes.get(fieldNames.longitude()));
            if (id == null || latitude == null || longitude == null) {
                continue;
            }
            outlets.add(new OutletStatus(
                    id,
                    asString(attributes.get(fieldNames.receivingWater())),
                    latitude,
                    longitude,
                    EnvironmentalAlertSeverity.WARNING,
                    "Active",
                    parseEpochMillis(attributes.get(fieldNames.eventStart())),
                    parseEpochMillis(attributes.get(fieldNames.eventEnd()))));
        }
        return outlets;
    }

    @SuppressWarnings("unchecked")
    private List<OutletStatus> parseWelshWaterFeatures(Map<String, Object> payload) {
        if (payload == null || !(payload.get("features") instanceof List<?> features)) {
            return List.of();
        }
        List<OutletStatus> outlets = new ArrayList<>();
        for (Object featureObject : features) {
            if (!(featureObject instanceof Map<?, ?> feature)) {
                continue;
            }
            Object attributesNode = feature.get("attributes");
            if (!(attributesNode instanceof Map<?, ?> attributesRaw)) {
                continue;
            }
            Map<String, Object> attributes = (Map<String, Object>) attributesRaw;
            String statusText = asString(attributes.get(fieldNames.status()));
            EnvironmentalAlertSeverity severity;
            String activityLabel;
            if (WELSH_WATER_STATUS_OPERATING.equals(statusText)) {
                severity = EnvironmentalAlertSeverity.WARNING;
                activityLabel = "Active";
            } else if (WELSH_WATER_STATUS_RECENT.equals(statusText)) {
                severity = EnvironmentalAlertSeverity.CAUTION;
                activityLabel = "Recent";
            } else {
                continue;
            }

            String id = resolveId(attributes);
            double[] latLon = parseGeometry(feature.get("geometry"));
            if (id == null || latLon == null) {
                continue;
            }
            outlets.add(new OutletStatus(
                    id,
                    asString(attributes.get(fieldNames.receivingWater())),
                    latLon[0],
                    latLon[1],
                    severity,
                    activityLabel,
                    parseLondonLocalDateTime(attributes.get(fieldNames.eventStart())),
                    parseLondonLocalDateTime(attributes.get(fieldNames.eventEnd()))));
        }
        return outlets;
    }

    private String resolveId(Map<String, Object> attributes) {
        String primary = asString(attributes.get(fieldNames.id()));
        if (primary != null) {
            return primary;
        }
        if (fieldNames.secondaryId() != null) {
            String secondary = asString(attributes.get(fieldNames.secondaryId()));
            if (secondary != null) {
                return secondary;
            }
        }
        if (fieldNames.tertiaryId() != null) {
            return asString(attributes.get(fieldNames.tertiaryId()));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static double[] parseGeometry(Object geometryNode) {
        if (!(geometryNode instanceof Map<?, ?> geometry)) {
            return null;
        }
        Double xMetres = parseDouble(((Map<String, Object>) geometry).get("x"));
        Double yMetres = parseDouble(((Map<String, Object>) geometry).get("y"));
        if (xMetres == null || yMetres == null) {
            return null;
        }
        return GeoDistance.webMercatorToLatLon(xMetres, yMetres);
    }

    private EnvironmentalAlertCandidate toCandidate(OutletStatus outlet) {
        Instant detectedAt = outlet.eventStart() != null ? outlet.eventStart() : Instant.now();
        Instant expiresAt = outlet.eventEnd() != null
                ? outlet.eventEnd().plus(48, ChronoUnit.HOURS)
                : Instant.now().plus(2, ChronoUnit.HOURS);
        String waterLabel =
                outlet.receivingWater() != null ? outlet.receivingWater() : "a nearby watercourse";
        String description = outlet.activityLabel()
                + " storm overflow ("
                + outlet.id()
                + ") into "
                + waterLabel
                + ". Think twice before paddling out - illness risk can stay elevated"
                + " for up to 48 hours after a discharge stops.";

        return new EnvironmentalAlertCandidate(
                EnvironmentalAlertType.SEWAGE_OVERFLOW,
                outlet.severity(),
                "Sewage pollution alert",
                description,
                config.getSourceName(),
                config.getSourceUrl(),
                outlet.id(),
                detectedAt,
                expiresAt);
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Double parseDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Instant parseEpochMillis(Object value) {
        if (value instanceof Number number) {
            long millis = number.longValue();
            if (millis <= 0) {
                return null;
            }
            return Instant.ofEpochMilli(millis);
        }
        return null;
    }

    private static Instant parseLondonLocalDateTime(Object value) {
        String text = asString(value);
        if (text == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(text).atZone(LONDON_ZONE).toInstant();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    record FieldNames(
            String status,
            String id,
            String secondaryId,
            String tertiaryId,
            String latitude,
            String longitude,
            String receivingWater,
            String eventStart,
            String eventEnd) {

        static FieldNames forStyle(String fieldStyle) {
            if (fieldStyle != null && fieldStyle.equalsIgnoreCase("southern")) {
                return new FieldNames(
                        "Status",
                        "Id",
                        null,
                        null,
                        "Latitude",
                        "Longitude",
                        "ReceivingWaterCourse",
                        "LatestEventStart",
                        "LatestEventEnd");
            }
            if (fieldStyle != null && fieldStyle.equalsIgnoreCase("welsh-water")) {
                return new FieldNames(
                        "status",
                        "DCWW_ID",
                        "permit_number",
                        "asset_name",
                        null,
                        null,
                        "Receiving_Water",
                        "start_date_time_discharge",
                        "stop_date_time_discharge");
            }
            return new FieldNames(
                    "status",
                    "Id",
                    null,
                    null,
                    "latitude",
                    "longitude",
                    "receivingWaterCourse",
                    "latestEventStart",
                    "latestEventEnd");
        }
    }

    private record OutletStatus(
            String id,
            String receivingWater,
            double latitude,
            double longitude,
            EnvironmentalAlertSeverity severity,
            String activityLabel,
            Instant eventStart,
            Instant eventEnd) {}

    private record CachedFeed(Instant fetchedAt, List<OutletStatus> outlets) {}
}
