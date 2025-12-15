-- Add missing indexes for better query performance
-- Only includes indexes that are actively used in queries

-- Slug indexes (for lookup queries)
-- These are frequently queried fields used in findBySlug() methods
CREATE INDEX IF NOT EXISTS idx_surf_spot_slug ON surf_spot(slug);
CREATE INDEX IF NOT EXISTS idx_region_slug ON region(slug);
CREATE INDEX IF NOT EXISTS idx_sub_region_slug ON sub_region(slug);
CREATE INDEX IF NOT EXISTS idx_country_slug ON country(slug);
CREATE INDEX IF NOT EXISTS idx_continent_slug ON continent(slug);

-- Collection table indexes (for join performance with @ElementCollection)
-- These tables are actively joined when filtering surf spots (hazards, facilities, foodOptions, accommodationOptions)
-- Used in SurfSpotRepositoryImpl.addCommonPredicates() with SetJoin operations
CREATE INDEX IF NOT EXISTS idx_surfspot_food_options_surfspot ON surfspot_food_options(surfspot_id);
CREATE INDEX IF NOT EXISTS idx_surfspot_accommodation_options_surfspot ON surfspot_accommodation_options(surfspot_id);
CREATE INDEX IF NOT EXISTS idx_surfspot_facilities_surfspot ON surfspot_facilities(surfspot_id);
CREATE INDEX IF NOT EXISTS idx_surfspot_hazards_surfspot ON surfspot_hazards(surfspot_id);
CREATE INDEX IF NOT EXISTS idx_surf_spot_forecasts_surf_spot ON surf_spot_forecasts(surf_spot_id);

-- Composite index for private spots filtering (status + created_by)
-- Used in SurfSpotRepositoryImpl.addPrivateSpotsFilters() - filters by status AND created_by together
-- This composite is more efficient than separate indexes for this query pattern
CREATE INDEX IF NOT EXISTS idx_surf_spot_status_created_by ON surf_spot(status, created_by);

