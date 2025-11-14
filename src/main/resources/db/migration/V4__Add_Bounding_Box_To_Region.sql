-- Add bounding_box column to region table
-- Bounding box: array of 4 coordinates [minLongitude, minLatitude, maxLongitude, maxLatitude]
-- Used for efficient spatial queries using simple array comparisons
ALTER TABLE region ADD COLUMN IF NOT EXISTS bounding_box double precision[];

