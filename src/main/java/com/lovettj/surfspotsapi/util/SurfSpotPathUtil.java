package com.lovettj.surfspotsapi.util;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SurfSpot;

/**
 * Builds the same URL path segment the app uses for surf spot detail routes.
 */
public final class SurfSpotPathUtil {

    private SurfSpotPathUtil() {}

    public static String pathFor(SurfSpot surfSpot) {
        Region region = surfSpot.getRegion();
        Country country = region != null ? region.getCountry() : null;
        Continent continent = country != null ? country.getContinent() : null;

        if (continent == null || country == null || region == null) {
            throw new IllegalStateException("Unable to generate surf spot path: missing continent/country/region");
        }

        String continentSlug = requireNonBlank(continent.getSlug(), "continent slug");
        String countrySlug = requireNonBlank(country.getSlug(), "country slug");
        String regionSlug = requireNonBlank(region.getSlug(), "region slug");
        String spotSlug = requireNonBlank(surfSpot.getSlug(), "surf spot slug");

        if (surfSpot.getSubRegion() != null) {
            String subRegionSlug = requireNonBlank(surfSpot.getSubRegion().getSlug(), "sub-region slug");
            return String.format(
                    "/surf-spots/%s/%s/%s/sub-regions/%s/%s",
                    continentSlug,
                    countrySlug,
                    regionSlug,
                    subRegionSlug,
                    spotSlug);
        }

        return String.format(
                "/surf-spots/%s/%s/%s/%s",
                continentSlug,
                countrySlug,
                regionSlug,
                spotSlug);
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Unable to generate surf spot path: missing " + label);
        }
        return value.trim();
    }
}
