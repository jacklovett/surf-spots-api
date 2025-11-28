package com.lovettj.surfspotsapi.service;

import com.lovettj.surfspotsapi.entity.SwellSeason;
import com.lovettj.surfspotsapi.repository.SwellSeasonRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to automatically determine the swell season for a surf spot based on its location.
 * Uses geographic coordinates (latitude/longitude) to determine which ocean region the spot is in.
 */
@Service
public class SwellSeasonDeterminationService {

    private final SwellSeasonRepository swellSeasonRepository;

    public SwellSeasonDeterminationService(SwellSeasonRepository swellSeasonRepository) {
        this.swellSeasonRepository = swellSeasonRepository;
    }

    /**
     * Determines the swell season for a surf spot based on its coordinates.
     * 
     * @param latitude The latitude of the surf spot
     * @param longitude The longitude of the surf spot
     * @return Optional SwellSeason if a match is found, empty otherwise
     */
    public Optional<SwellSeason> determineSwellSeason(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }

        // Determine ocean region based on coordinates
        String regionName = determineOceanRegion(latitude, longitude);
        
        if (regionName == null) {
            return Optional.empty();
        }

        // Find the swell season by name
        return swellSeasonRepository.findByName(regionName);
    }

    /**
     * Determines which ocean region a location belongs to based on coordinates.
     * Uses geographic boundaries to classify locations into swell season regions.
     * 
     * @param latitude Latitude of the location
     * @param longitude Longitude of the location
     * @return The name of the ocean region, or null if not determinable
     */
    private String determineOceanRegion(double latitude, double longitude) {
        // Check specific seas first (more specific boundaries)
        
        // North Sea (UK, Netherlands, Germany, Denmark, Norway)
        if (isInNorthSea(latitude, longitude)) {
            return "North Sea";
        }

        // Baltic Sea (Sweden, Finland, Estonia, Latvia, Lithuania, Poland, Germany)
        if (isInBalticSea(latitude, longitude)) {
            return "Baltic Sea";
        }

        // Mediterranean (Mediterranean countries)
        if (isInMediterranean(latitude, longitude)) {
            return "Mediterranean";
        }

        // Red Sea (Red Sea region)
        if (isInRedSea(latitude, longitude)) {
            return "Red Sea";
        }

        // Arabian Sea (Arabian Peninsula, Pakistan, Western India)
        if (isInArabianSea(latitude, longitude)) {
            return "Arabian Sea";
        }

        // Bay of Bengal (Eastern India, Bangladesh, Myanmar, Thailand)
        if (isInBayOfBengal(latitude, longitude)) {
            return "Bay of Bengal";
        }

        // Central America Pacific (Pacific coast of Central America) - check before Caribbean
        if (isInCentralAmericaPacific(latitude, longitude)) {
            return "Central America Pacific";
        }

        // Gulf of Mexico (Gulf Coast US, Mexico)
        if (isInGulfOfMexico(latitude, longitude)) {
            return "Gulf of Mexico";
        }

        // Caribbean (Caribbean islands and surrounding areas)
        if (isInCaribbean(latitude, longitude)) {
            return "Caribbean";
        }

        // Tasman Sea (East Coast Australia, New Zealand)
        if (isInTasmanSea(latitude, longitude)) {
            return "Tasman Sea";
        }

        // Coral Sea (Northeast Australia, Papua New Guinea, Solomon Islands)
        if (isInCoralSea(latitude, longitude)) {
            return "Coral Sea";
        }

        // Indonesia (Indonesian archipelago) - check before South China Sea to avoid overlap
        if (isInIndonesia(latitude, longitude)) {
            return "Indonesia";
        }

        // South China Sea (Vietnam, Philippines, Malaysia, Southern China)
        // Checked after Indonesia so Indonesian islands match Indonesia, not South China Sea
        if (isInSouthChinaSea(latitude, longitude)) {
            return "South China Sea";
        }

        // Japan / Northwest Pacific (Japan, Korea, Taiwan, Philippines)
        // Checked after South China Sea - Philippines overlap handled by South China Sea check first
        if (isInJapanNorthwestPacific(latitude, longitude)) {
            return "Japan / Northwest Pacific";
        }

        // Then check broader ocean regions
        
        // North Atlantic (Europe, East Coast US/Canada)
        if (isInNorthAtlantic(latitude, longitude)) {
            return "North Atlantic";
        }

        // North Pacific (West Coast US/Canada, Hawaii, Alaska)
        if (isInNorthPacific(latitude, longitude)) {
            return "North Pacific";
        }

        // South Pacific (Australia, New Zealand, Pacific Islands)
        if (isInSouthPacific(latitude, longitude)) {
            return "South Pacific";
        }

        // Indian Ocean (Indonesia, Maldives, India, Sri Lanka, East Africa)
        if (isInIndianOcean(latitude, longitude)) {
            return "Indian Ocean";
        }

        // West Africa Atlantic (West African coast)
        if (isInWestAfricaAtlantic(latitude, longitude)) {
            return "West Africa Atlantic";
        }

        // South Atlantic (South America, South Africa)
        if (isInSouthAtlantic(latitude, longitude)) {
            return "South Atlantic";
        }


        return null;
    }

    // Geographic region determination methods
    // Based on IHO (International Hydrographic Organization) boundaries and documented sea/ocean boundaries

    private boolean isInNorthAtlantic(double lat, double lon) {
        // North Atlantic Ocean boundaries (IHO definition)
        // Europe coast: 35-71N, 10W-30E (from Portugal to Norway)
        // East Coast US/Canada: 25-50N, 80-50W (from Florida to Newfoundland)
        // Note: Specific seas (Mediterranean, North Sea, Baltic, Caribbean, Gulf of Mexico) are checked first
        // Caribbean (9-25N, -85 to -55W) and Gulf of Mexico (18-31N, -98 to -85W) are excluded
        return (lat >= 35 && lat <= 71 && lon >= -10 && lon <= 30) ||
               (lat >= 25 && lat <= 50 && lon >= -80 && lon <= -50);
    }

    private boolean isInNorthSea(double lat, double lon) {
        // North Sea boundaries: 51-62N, 4W-9E
        // Bounded by UK, Norway, Denmark, Germany, Netherlands, Belgium
        return lat >= 51 && lat <= 62 && lon >= -4 && lon <= 9;
    }

    private boolean isInBalticSea(double lat, double lon) {
        // Baltic Sea boundaries: 54-66N, 9-30E
        // Bounded by Sweden, Finland, Estonia, Latvia, Lithuania, Poland, Germany, Denmark
        return lat >= 54 && lat <= 66 && lon >= 9 && lon <= 30;
    }

    private boolean isInMediterranean(double lat, double lon) {
        // Mediterranean Sea boundaries: 30-46N, 6W-36E
        // Includes: Western Mediterranean, Adriatic, Ionian, Aegean, Levantine
        // Excludes: Black Sea (separate)
        return lat >= 30 && lat <= 46 && lon >= -6 && lon <= 36;
    }

    private boolean isInCaribbean(double lat, double lon) {
        // Caribbean Sea boundaries: 9-25N, 85-55W (east of Central America isthmus)
        // Bounded by Central America, Greater Antilles (Cuba, Jamaica, Hispaniola, Puerto Rico),
        // Lesser Antilles (including Barbados), South America
        // Exclude Pacific coast (west of -82W) to avoid overlap with Central America Pacific
        // Exclude Gulf of Mexico (west of -85W) - Gulf is checked first for Gulf Coast US/Mexico
        // Eastern boundary at -55W to include Barbados (-59.5432W) and other eastern Caribbean islands
        return lat >= 9 && lat <= 25 && lon > -85 && lon <= -55;
    }

    private boolean isInGulfOfMexico(double lat, double lon) {
        // Gulf of Mexico boundaries: 18-31N, 98-85W
        // Bounded by US (Texas, Louisiana, Mississippi, Alabama, Florida), Mexico
        // Excludes Caribbean islands (Cuba, Florida Keys) which are east of -85W
        // Note: Caribbean is checked after this, so Caribbean islands won't match here
        return lat >= 18 && lat <= 31 && lon >= -98 && lon <= -85;
    }

    private boolean isInNorthPacific(double lat, double lon) {
        // North Pacific Ocean boundaries (IHO definition)
        // West Coast US/Canada: 32-60N, 125-105W (California to British Columbia)
        // Hawaii: 18-23N, 161-154W
        // Alaska: 51-72N, 180-130W (wrapping around date line)
        // Note: Specific regions (Central America Pacific, Japan/Northwest Pacific) are checked first
        return (lat >= 32 && lat <= 60 && lon >= -125 && lon <= -105) ||
               (lat >= 18 && lat <= 23 && lon >= -161 && lon <= -154) ||
               (lat >= 51 && lat <= 72 && (lon <= -130 || lon >= 170));
    }

    private boolean isInCentralAmericaPacific(double lat, double lon) {
        // Central America Pacific coast: 7-20N, 110-82W
        // From Mexico (south of Baja) to Panama Pacific coast
        // Includes Costa Rica (-84.8W) and extends to Panama (-82W)
        return lat >= 7 && lat <= 20 && lon >= -110 && lon <= -82;
    }

    private boolean isInSouthPacific(double lat, double lon) {
        // South Pacific Ocean boundaries (IHO definition)
        // Australia (Pacific-facing coasts): 10-45S, 113-155E
        // New Zealand: 34-47S, 166-179E
        // Pacific Islands: 0-30S, 150E-180W (wrapping around date line)
        // Note: Specific seas (Tasman Sea, Coral Sea) are checked first
        return (lat >= -45 && lat <= -10 && lon >= 113 && lon <= 155) ||
               (lat >= -47 && lat <= -34 && lon >= 166 && lon <= 179) ||
               (lat >= -30 && lat <= 0 && (lon >= 150 || lon <= -150));
    }

    private boolean isInIndianOcean(double lat, double lon) {
        // Indian Ocean boundaries (IHO definition)
        // West boundary: 20E (Africa coast)
        // East boundary: 146E (Australia west coast, Indonesia)
        // North boundary: 30N (Arabian Peninsula, India, Southeast Asia)
        // South boundary: 60S (Antarctica)
        // Note: Specific seas (Red Sea, Arabian Sea, Bay of Bengal, South China Sea, Indonesia) are checked first
        return lat >= -60 && lat <= 30 && lon >= 20 && lon <= 146;
    }

    private boolean isInRedSea(double lat, double lon) {
        // Red Sea boundaries: 12-30N, 32-43E
        // Bounded by Egypt, Sudan, Eritrea, Saudi Arabia, Yemen
        return lat >= 12 && lat <= 30 && lon >= 32 && lon <= 43;
    }

    private boolean isInArabianSea(double lat, double lon) {
        // Arabian Sea boundaries: 5-25N, 50-78E
        // Bounded by Arabian Peninsula, Pakistan, India (west coast), Somalia
        return lat >= 5 && lat <= 25 && lon >= 50 && lon <= 78;
    }

    private boolean isInBayOfBengal(double lat, double lon) {
        // Bay of Bengal boundaries: 5-22N, 80-95E
        // Bounded by India (east coast), Bangladesh, Myanmar, Thailand, Sri Lanka
        return lat >= 5 && lat <= 22 && lon >= 80 && lon <= 95;
    }

    private boolean isInWestAfricaAtlantic(double lat, double lon) {
        // West Africa Atlantic coast: 5S-35N, 20W-10E
        // From South Africa (west) to Morocco (Atlantic coast)
        // Note: Mediterranean coast regions are checked first
        return lat >= -5 && lat <= 35 && lon >= -20 && lon <= 10;
    }

    private boolean isInSouthAtlantic(double lat, double lon) {
        // South Atlantic Ocean boundaries (IHO definition)
        // South America (East Coast): 5S-55S, 50-35W (from Brazil to Argentina)
        // South Africa (west coast): 25-35S, 15-20E
        // Namibia: 17-29S, 11-20E
        return (lat >= -55 && lat <= -5 && lon >= -50 && lon <= -35) ||
               (lat >= -35 && lat <= -17 && lon >= 11 && lon <= 20);
    }

    private boolean isInTasmanSea(double lat, double lon) {
        // Tasman Sea boundaries: 25-47S, 150-167E
        // Between Australia (east coast) and New Zealand
        return lat >= -47 && lat <= -25 && lon >= 150 && lon <= 167;
    }

    private boolean isInCoralSea(double lat, double lon) {
        // Coral Sea boundaries: 10-25S, 145-165E
        // Between Australia (northeast), Papua New Guinea, Solomon Islands, New Caledonia
        return lat >= -25 && lat <= -10 && lon >= 145 && lon <= 165;
    }

    private boolean isInJapanNorthwestPacific(double lat, double lon) {
        // Japan / Northwest Pacific boundaries
        // Japan: 24-46N, 123-146E
        // Korea: 33-43N, 124-132E
        // Taiwan: 22-25N, 120-122E
        // Philippines: 5-21N, 117-127E
        // East China Sea: 23-41N, 117-131E
        return (lat >= 24 && lat <= 46 && lon >= 123 && lon <= 146) ||
               (lat >= 33 && lat <= 43 && lon >= 124 && lon <= 132) ||
               (lat >= 22 && lat <= 25 && lon >= 120 && lon <= 122) ||
               (lat >= 5 && lat <= 21 && lon >= 117 && lon <= 127) ||
               (lat >= 23 && lat <= 41 && lon >= 117 && lon <= 131);
    }

    private boolean isInSouthChinaSea(double lat, double lon) {
        // South China Sea boundaries: 0-25N, 100-120E
        // Bounded by China (south), Vietnam, Philippines, Malaysia, Brunei
        // Note: Indonesia is checked first, so Indonesian islands won't match here
        // Note: Japan/Northwest Pacific is checked after, but Philippines overlap is handled by checking South China Sea first
        return lat >= 0 && lat <= 25 && lon >= 100 && lon <= 120;
    }

    private boolean isInIndonesia(double lat, double lon) {
        // Indonesia archipelago boundaries: 11S-6N, 95-141E
        // Includes: Sumatra, Java, Bali, Lombok, Sumbawa, Flores, Sulawesi, Maluku, Papua
        // This region has both Indian Ocean and Pacific Ocean coasts
        // Note: Checked before South China Sea to ensure Indonesian islands match here, not South China Sea
        return lat >= -11 && lat <= 6 && lon >= 95 && lon <= 141;
    }
}

