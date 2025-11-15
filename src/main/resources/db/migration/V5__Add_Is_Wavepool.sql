-- Add is_wavepool and wavepool_url columns to surf_spot table
ALTER TABLE surf_spot ADD COLUMN IF NOT EXISTS is_wavepool BOOLEAN DEFAULT false;
ALTER TABLE surf_spot ADD COLUMN IF NOT EXISTS wavepool_url VARCHAR(500);
