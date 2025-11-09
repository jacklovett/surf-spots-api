-- Remove duplicate entries from user_surf_spot, keeping only the oldest one for each user/surf_spot combination
-- This handles existing duplicates before adding the unique constraint
WITH ranked_entries AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id, surf_spot_id ORDER BY created_at ASC, id ASC) as rn
    FROM user_surf_spot
)
DELETE FROM user_surf_spot
WHERE id IN (
    SELECT id FROM ranked_entries WHERE rn > 1
);

-- Add unique constraint to prevent future duplicates in user_surf_spot
ALTER TABLE user_surf_spot
ADD CONSTRAINT unique_user_surf_spot UNIQUE (user_id, surf_spot_id);

-- Remove duplicate entries from watch_list_surf_spot, keeping only the oldest one for each user/surf_spot combination
WITH ranked_entries AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id, surf_spot_id ORDER BY created_at ASC, id ASC) as rn
    FROM watch_list_surf_spot
)
DELETE FROM watch_list_surf_spot
WHERE id IN (
    SELECT id FROM ranked_entries WHERE rn > 1
);

-- Add unique constraint to prevent future duplicates in watch_list_surf_spot
ALTER TABLE watch_list_surf_spot
ADD CONSTRAINT unique_watch_list_surf_spot UNIQUE (user_id, surf_spot_id);

