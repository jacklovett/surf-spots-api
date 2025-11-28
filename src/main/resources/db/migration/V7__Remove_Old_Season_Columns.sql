-- Remove old season_start and season_end columns from surf_spot table
-- Note: This migration should only run after data has been migrated to use swell_season_id
ALTER TABLE surf_spot DROP COLUMN IF EXISTS season_start;
ALTER TABLE surf_spot DROP COLUMN IF EXISTS season_end;

