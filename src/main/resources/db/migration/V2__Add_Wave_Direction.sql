-- Add wave_direction column to surf_spot table
ALTER TABLE surf_spot ADD COLUMN IF NOT EXISTS wave_direction VARCHAR(50);

-- Drop any existing check constraint on wave_direction (if Hibernate created one)
ALTER TABLE surf_spot DROP CONSTRAINT IF EXISTS surf_spot_wave_direction_check;

-- Add check constraint that allows the enum constant names
ALTER TABLE surf_spot ADD CONSTRAINT surf_spot_wave_direction_check 
    CHECK (wave_direction IS NULL OR wave_direction IN ('LEFT', 'RIGHT', 'LEFT_AND_RIGHT'));

-- Set default wave direction for existing records based on break type
-- Beach breaks typically break both ways, point breaks are usually one direction
-- This is a best-guess default - accurate data should come from seed data or manual updates
UPDATE surf_spot 
SET wave_direction = 'LEFT_AND_RIGHT' 
WHERE wave_direction IS NULL AND type = 'Beach Break';

-- For point breaks, set a default (most point breaks are rights, but this is a guess)
-- Users should update these manually for accuracy
UPDATE surf_spot 
SET wave_direction = 'RIGHT' 
WHERE wave_direction IS NULL AND type = 'Point Break';

-- For reef breaks, default to 'LEFT_AND_RIGHT' as many can break both ways
UPDATE surf_spot 
SET wave_direction = 'LEFT_AND_RIGHT' 
WHERE wave_direction IS NULL AND type = 'Reef Break';

-- Update known seeded spots with their correct wave directions from seed data
UPDATE surf_spot SET wave_direction = 'RIGHT' WHERE name = 'Ribeira d''Ilhas';
UPDATE surf_spot SET wave_direction = 'RIGHT' WHERE name = 'Coxos';
UPDATE surf_spot SET wave_direction = 'RIGHT' WHERE name = 'Thurso East';
UPDATE surf_spot SET wave_direction = 'RIGHT' WHERE name = 'Cabo Ledo';
UPDATE surf_spot SET wave_direction = 'LEFT' WHERE name = 'Mundaka';

-- Catch-all: set any remaining NULL values to 'LEFT_AND_RIGHT' (most common)
UPDATE surf_spot 
SET wave_direction = 'LEFT_AND_RIGHT' 
WHERE wave_direction IS NULL;
