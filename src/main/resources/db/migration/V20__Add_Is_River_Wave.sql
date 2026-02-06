-- Add is_river_wave to surf_spot table (river wave = standing wave / river break, like wavepools often no natural swell season)
ALTER TABLE surf_spot ADD COLUMN IF NOT EXISTS is_river_wave BOOLEAN DEFAULT false;
