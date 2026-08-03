package com.lovettj.surfspotsapi.integration.environmental.uk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.lovettj.surfspotsapi.integration.environmental.uk.ScottishWaterFeedProperties;

/**
 * Cached client for Scottish Water near-real-time overflow feed.
 */
@Component
@ConditionalOnProperty(name = "app.environmental-alerts.enabled", havingValue = "true")
public class ScottishWaterOverflowFeedClient {

    private static final Logger logger = LoggerFactory.getLogger(ScottishWaterOverflowFeedClient.class);

    static final int STATUS_OVERFLOWING = 13;
    static final int STATUS_RECENT_OVERFLOW = 14;

    private final RestClient restClient;
    private final UkOverflowProperties ukConfig;
    private final ScottishWaterFeedProperties config;
    private final AtomicReference<CachedFeed> cache = new AtomicReference<>();

    public ScottishWaterOverflowFeedClient(
            RestClient environmentalAlertRestClient, UkOverflowProperties properties) {
        this.restClient = environmentalAlertRestClient;
        this.ukConfig = properties;
        this.config = properties.getScottishWater();
    }

    public List<OutletStatus> fetchActiveOrRecentOutlets() {
        CachedFeed cached = cache.get();
        Instant now = Instant.now();
        if (cached != null
                && cached.fetchedAt()
                        .plus(ukConfig.getFeedCacheTtl())
                        .isAfter(now)) {
            return cached.outlets();
        }

        String apiUrl = config.getApiUrl();
        Map<String, Object> payload = restClient
                .get()
                .uri(apiUrl)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        List<OutletStatus> outlets = parseOutlets(payload);
        cache.set(new CachedFeed(now, outlets));
        logger.info("Scottish Water overflow feed refreshed activeOrRecent={}", outlets.size());
        return outlets;
    }

    @SuppressWarnings("unchecked")
    private static List<OutletStatus> parseOutlets(Map<String, Object> payload) {
        if (payload == null) {
            return List.of();
        }
        Object resultsNode = payload.get("results");
        if (!(resultsNode instanceof List<?> results)) {
            return List.of();
        }

        List<OutletStatus> outlets = new ArrayList<>();
        for (Object rowObject : results) {
            if (!(rowObject instanceof Map<?, ?> row)) {
                continue;
            }
            Map<String, Object> fields = (Map<String, Object>) row;
            int statusId = parseInt(fields.get("OVERFLOW_STATUS_ID"), -1);
            if (statusId != STATUS_OVERFLOWING && statusId != STATUS_RECENT_OVERFLOW) {
                continue;
            }
            String assetId = asString(fields.get("ASSET_ID"));
            Double latitude = parseDouble(fields.get("DISCHARGE_OVERFLOW_LOCATION_LATITUDE"));
            Double longitude = parseDouble(fields.get("DISCHARGE_OVERFLOW_LOCATION_LONGITUDE"));
            if (assetId == null || assetId.isBlank() || latitude == null || longitude == null) {
                continue;
            }
            outlets.add(new OutletStatus(
                    assetId,
                    asString(fields.get("ASSET_NAME")),
                    asString(fields.get("RECEIVING_WATER")),
                    asString(fields.get("OVERFLOW_STATUS_DESCRIPTION")),
                    statusId,
                    latitude,
                    longitude,
                    parseInstant(fields.get("OVERFLOW_START_DATETIME")),
                    parseInstant(fields.get("OVERFLOW_END_DATETIME"))));
        }
        return outlets;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static int parseInt(Object value, int fallback) {
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
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Instant parseInstant(Object value) {
        String text = asString(value);
        if (text == null) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public record OutletStatus(
            String assetId,
            String assetName,
            String receivingWater,
            String statusDescription,
            int statusId,
            double latitude,
            double longitude,
            Instant overflowStart,
            Instant overflowEnd) {}

    private record CachedFeed(Instant fetchedAt, List<OutletStatus> outlets) {}
}
