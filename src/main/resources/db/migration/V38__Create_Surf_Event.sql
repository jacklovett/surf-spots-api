CREATE TABLE surf_event (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(32) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    location_name   VARCHAR(255) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    status          VARCHAR(64) NOT NULL DEFAULT 'SCHEDULED',
    surf_spot_id    BIGINT REFERENCES surf_spot(id) ON DELETE SET NULL,
    source          VARCHAR(64) NOT NULL DEFAULT 'MANUAL',
    created_at      TIMESTAMP,
    modified_at     TIMESTAMP
);

CREATE TABLE surf_event_contest_detail (
    surf_event_id        BIGINT PRIMARY KEY REFERENCES surf_event(id) ON DELETE CASCADE,
    organizer            VARCHAR(64) NOT NULL,
    series               VARCHAR(128),
    season_year          INTEGER NOT NULL,
    venue_location_key   VARCHAR(255) NOT NULL,
    UNIQUE (organizer, series, venue_location_key, season_year)
);

CREATE INDEX idx_surf_event_surf_spot_id ON surf_event (surf_spot_id);
CREATE INDEX idx_surf_event_type_status ON surf_event (event_type, status);
CREATE INDEX idx_surf_event_contest_venue_year ON surf_event_contest_detail (venue_location_key, season_year);

ALTER TABLE surf_event
    ADD CONSTRAINT surf_event_status_check
        CHECK (status IN ('SCHEDULED', 'UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED'));
