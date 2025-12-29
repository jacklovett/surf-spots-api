-- Add media_type column to surfboard_images table to support both images and videos
ALTER TABLE surfboard_images
ADD COLUMN media_type VARCHAR(50) NOT NULL DEFAULT 'image';

-- Update existing records to have 'image' as media type (already set by default, but explicit for clarity)
UPDATE surfboard_images
SET media_type = 'image'
WHERE media_type IS NULL OR media_type = '';

