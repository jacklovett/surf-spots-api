-- Preferred display units for signed-in users (metric | imperial).
-- Guests keep units in browser localStorage only.
ALTER TABLE user_settings
    ADD COLUMN preferred_units VARCHAR(16) NOT NULL DEFAULT 'metric';

ALTER TABLE user_settings
    ADD CONSTRAINT chk_user_settings_preferred_units
        CHECK (preferred_units IN ('metric', 'imperial'));
