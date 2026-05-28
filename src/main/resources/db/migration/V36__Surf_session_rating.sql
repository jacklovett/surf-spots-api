-- Replace wave_quality enum and would_surf_again with a 1-5 session rating,
-- and add an optional wave_face column for surface feel.
ALTER TABLE surf_session
    ADD COLUMN session_rating INTEGER,
    ADD COLUMN wave_face VARCHAR(50);

UPDATE surf_session
SET session_rating = CASE wave_quality
    WHEN 'POOR' THEN 1
    WHEN 'OKAY' THEN 2
    WHEN 'FUN' THEN 4
    WHEN 'GREAT' THEN 5
    ELSE NULL
END;

ALTER TABLE surf_session
    ADD CONSTRAINT surf_session_session_rating_check
        CHECK (session_rating IS NULL OR (session_rating >= 1 AND session_rating <= 5));

ALTER TABLE surf_session
    DROP COLUMN wave_quality,
    DROP COLUMN would_surf_again;
