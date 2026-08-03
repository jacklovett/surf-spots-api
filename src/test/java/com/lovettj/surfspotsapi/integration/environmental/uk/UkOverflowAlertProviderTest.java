package com.lovettj.surfspotsapi.integration.environmental.uk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import com.lovettj.surfspotsapi.dto.EnvironmentalAlertCandidate;
import com.lovettj.surfspotsapi.entity.Country;
import com.lovettj.surfspotsapi.entity.Region;
import com.lovettj.surfspotsapi.entity.SubRegion;
import com.lovettj.surfspotsapi.entity.SurfSpot;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertSeverity;
import com.lovettj.surfspotsapi.enums.EnvironmentalAlertType;
import com.lovettj.surfspotsapi.integration.environmental.uk.ScottishWaterOverflowFeedClient.OutletStatus;

@ExtendWith(MockitoExtension.class)
class UkOverflowAlertProviderTest {

    @Mock
    private ScottishWaterOverflowFeedClient scottishFeed;

    @Mock
    private RestClient restClient;

    private UkOverflowAlertProvider provider;

    @BeforeEach
    void setUp() {
        UkOverflowProperties properties = new UkOverflowProperties();
        properties.setMatchRadiusMetres(3000);
        properties.getScottishWater().setSourceName("Scottish Water");
        properties.getScottishWater().setSourceUrl("https://www.scottishwater.co.uk/");
        properties.setStreamCompanies(List.of());
        provider = new UkOverflowAlertProvider(properties, scottishFeed, restClient);
    }

    @Test
    void supportsShouldMatchUnitedKingdomCountrySlug() {
        Country unitedKingdom = country("United Kingdom");
        Country france = country("France");
        Country englandAsCountry = country("England");

        assertTrue(provider.supports(unitedKingdom));
        assertFalse(provider.supports(france));
        assertFalse(provider.supports(englandAsCountry));
    }

    @Test
    void getProviderKeyShouldBeUkOverflow() {
        assertEquals("uk-overflow", provider.getProviderKey());
    }

    @Test
    void fetchAlertsShouldRouteScotlandNationRegionToScottishFeed() {
        when(scottishFeed.fetchActiveOrRecentOutlets())
                .thenReturn(List.of(new OutletStatus(
                        "CSO000007",
                        "CROSSHOUSE CSO",
                        "Carmel Water",
                        "OF - Overflowing",
                        ScottishWaterOverflowFeedClient.STATUS_OVERFLOWING,
                        55.612948,
                        -4.5525256,
                        Instant.parse("2026-07-29T16:25:00Z"),
                        null)));

        SurfSpot spot = spotUnderNationRegion("Scotland", 55.612948, -4.5525256);
        List<EnvironmentalAlertCandidate> candidates = provider.fetchAlerts(spot);

        assertEquals(1, candidates.size());
        EnvironmentalAlertCandidate candidate = candidates.get(0);
        assertEquals(EnvironmentalAlertType.SEWAGE_OVERFLOW, candidate.type());
        assertEquals(EnvironmentalAlertSeverity.WARNING, candidate.severity());
        assertEquals("Scottish Water", candidate.sourceName());
        assertEquals("CSO000007", candidate.externalId());
    }

    @Test
    void fetchAlertsShouldRouteViaSubRegionParentNation() {
        when(scottishFeed.fetchActiveOrRecentOutlets())
                .thenReturn(List.of(new OutletStatus(
                        "CSO000007",
                        "CROSSHOUSE CSO",
                        "Carmel Water",
                        "OF - Overflowing",
                        ScottishWaterOverflowFeedClient.STATUS_OVERFLOWING,
                        55.612948,
                        -4.5525256,
                        Instant.parse("2026-07-29T16:25:00Z"),
                        null)));

        Country unitedKingdom = country("United Kingdom");
        Region scotland = region("Scotland", unitedKingdom);
        Region localArea = region("Caithness", unitedKingdom);
        SubRegion subRegion = SubRegion.builder().name("Thurso").region(scotland).build();
        SurfSpot spot = SurfSpot.builder()
                .id(1L)
                .name("Test spot")
                .latitude(55.612948)
                .longitude(-4.5525256)
                .region(localArea)
                .subRegion(subRegion)
                .build();

        assertEquals(1, provider.fetchAlerts(spot).size());
    }

    @Test
    void fetchAlertsShouldReturnEmptyForNorthernIrelandUntilLiveFeedExists() {
        SurfSpot spot = spotUnderNationRegion("Northern Ireland", 55.2, -6.6);
        assertTrue(provider.fetchAlerts(spot).isEmpty());
    }

    private static Country country(String name) {
        Country country = Country.builder().name(name).build();
        country.generateSlug();
        return country;
    }

    private static Region region(String name, Country country) {
        Region region = Region.builder().name(name).country(country).build();
        region.generateSlug();
        return region;
    }

    private static SurfSpot spotUnderNationRegion(
            String nationRegionName, double latitude, double longitude) {
        Country unitedKingdom = country("United Kingdom");
        Region nationRegion = region(nationRegionName, unitedKingdom);
        return SurfSpot.builder()
                .id(1L)
                .name("Test spot")
                .latitude(latitude)
                .longitude(longitude)
                .region(nationRegion)
                .build();
    }
}
