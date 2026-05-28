-- Drop legacy wave_quality and would_surf_again; add session_rating and wave_face.
ALTER TABLE surf_session
    DROP COLUMN wave_quality,
    DROP COLUMN would_surf_again,
    ADD COLUMN session_rating INTEGER,
    ADD COLUMN wave_face VARCHAR(50);

ALTER TABLE surf_session
    ADD CONSTRAINT surf_session_session_rating_check
        CHECK (session_rating IS NULL OR (session_rating >= 1 AND session_rating <= 5));
