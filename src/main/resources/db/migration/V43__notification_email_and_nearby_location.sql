-- Dedupes marketing/watchlist notification emails so scheduled jobs do not re-send.
CREATE TABLE notification_email_sent (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    notification_key VARCHAR(255) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_email_sent_user_key UNIQUE (user_id, notification_key)
);

CREATE INDEX idx_notification_email_sent_user_id ON notification_email_sent (user_id);

-- Last known browser/device location for nearby-travel alerts (web on-visit).
ALTER TABLE user_settings
    ADD COLUMN last_known_latitude DOUBLE PRECISION,
    ADD COLUMN last_known_longitude DOUBLE PRECISION,
    ADD COLUMN last_known_location_at TIMESTAMPTZ;
