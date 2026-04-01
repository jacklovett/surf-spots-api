package com.lovettj.surfspotsapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.lovettj.surfspotsapi.entity.Continent;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;

class SurfSpotPathUtilTests {

    @Test
    void pathForShouldBuildPathWithoutSubRegion() {
        Continent continent = new Continent();
        continent.setSlug("europe");
        Country country = new Country();
        country.setSlug("pt");
        country.setContinent(continent);
        Region region = new Region();
        region.setSlug("lisbon");
        region.setCountry(country);
        SurfSpot spot = SurfSpot.builder().name("Carcavelos").build();
        spot.setSlug("carcavelos");
        spot.setRegion(region);

        assertEquals("/surf-spots/europe/pt/lisbon/carcavelos", SurfSpotPathUtil.pathFor(spot));
    }

    @Test
    void pathForShouldIncludeSubRegionWhenPresent() {
        Continent continent = new Continent();
        continent.setSlug("europe");
        Country country = new Country();
        country.setSlug("es");
        country.setContinent(continent);
        Region region = new Region();
        region.setSlug("andalusia");
        region.setCountry(country);
        SubRegion sub = SubRegion.builder().name("Costa").build();
        sub.setSlug("costa");
        sub.setRegion(region);
        SurfSpot spot = SurfSpot.builder().name("Spot").build();
        spot.setSlug("spot");
        spot.setRegion(region);
        spot.setSubRegion(sub);

        assertEquals(
                "/surf-spots/europe/es/andalusia/sub-regions/costa/spot",
                SurfSpotPathUtil.pathFor(spot));
    }

    @Test
    void pathForShouldThrowWhenRegionMissing() {
        SurfSpot spot = SurfSpot.builder().name("X").build();
        spot.setSlug("x");

        assertThrows(IllegalStateException.class, () -> SurfSpotPathUtil.pathFor(spot));
    }
}
