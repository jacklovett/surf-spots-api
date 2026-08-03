package com.lovettj.surfspotsapi.integration.environmental;

import java.util.List;

import com.lovettj.surfspotsapi.dto.EnvironmentalAlertCandidate;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.SurfSpot;

/**
 * One country's (or region's) environmental data source.
 *
 * <p>To add a new provider later:
 * <ol>
 *   <li>Add a {@code @Component} class that implements this interface.</li>
 *   <li>Implement {@link #supports(Country)} using the country's slug (e.g. {@code portugal}).</li>
 *   <li>Implement {@link #fetchAlerts(SurfSpot)} to return normalised candidates for that spot.</li>
 *   <li>Add any URLs/settings as a {@code @ConfigurationProperties} class next to the provider.</li>
 * </ol>
 * Spring collects every implementation. Sync runs each provider that supports the spot's country.
 */
public interface EnvironmentalAlertProvider {

    /** Stable id for logs and dedupe (e.g. {@code uk-overflow}). */
    String getProviderKey();

    /** True when this provider covers the given country. */
    boolean supports(Country country);

    /** Fetch alert candidates for one watched surf spot. */
    List<EnvironmentalAlertCandidate> fetchAlerts(SurfSpot surfSpot);
}
