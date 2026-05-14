-- Session timing: UTC instants (same interchange model as wearables/partners).
-- surf_spot.iana_zone_id converts manual date+time submissions and derives local labels from instants.
ALTER TABLE surf_spot
    ADD COLUMN iana_zone_id VARCHAR(64);

ALTER TABLE surf_session
    ADD COLUMN duration_minutes INTEGER,
    ADD COLUMN session_start_instant TIMESTAMPTZ,
    ADD COLUMN session_end_instant TIMESTAMPTZ;
