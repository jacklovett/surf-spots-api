-- Session detail fields are optional; users may save with date (and spot) only.
ALTER TABLE surf_session
    ALTER COLUMN wave_size DROP NOT NULL,
    ALTER COLUMN crowd_level DROP NOT NULL,
    ALTER COLUMN wave_quality DROP NOT NULL,
    ALTER COLUMN would_surf_again DROP NOT NULL;
