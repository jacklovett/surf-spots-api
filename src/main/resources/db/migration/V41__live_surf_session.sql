-- Live surf sessions: lifecycle, optional GPS start, emergency-contact sharing, overdue alerts.
ALTER TABLE surf_session
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN start_latitude DOUBLE PRECISION,
    ADD COLUMN start_longitude DOUBLE PRECISION,
    ADD COLUMN share_location_with_emergency_contact BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN expected_return_instant TIMESTAMPTZ,
    ADD COLUMN overdue_notification_sent_at TIMESTAMPTZ,
    ADD COLUMN start_iana_zone_id VARCHAR(64);

ALTER TABLE surf_session ALTER COLUMN skill_level DROP NOT NULL;
ALTER TABLE surf_session ALTER COLUMN surf_spot_id DROP NOT NULL;

-- Legacy open-ended logs (V1-V40 imports): mark then close non-GPS rows so live-session rules apply cleanly.
UPDATE surf_session
SET status = 'IN_PROGRESS'
WHERE session_start_instant IS NOT NULL
  AND session_end_instant IS NULL;

UPDATE surf_session
SET status = 'COMPLETED',
    session_end_instant = COALESCE(session_end_instant, session_start_instant, created_at),
    duration_minutes = COALESCE(duration_minutes, 0)
WHERE status = 'IN_PROGRESS'
  AND start_latitude IS NULL
  AND start_longitude IS NULL;

WITH ranked_in_progress AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id
               ORDER BY session_start_instant DESC NULLS LAST, created_at DESC
           ) AS row_rank
    FROM surf_session
    WHERE status = 'IN_PROGRESS'
)
UPDATE surf_session session_row
SET status = 'COMPLETED',
    session_end_instant = COALESCE(
        session_row.session_end_instant,
        session_row.session_start_instant,
        session_row.created_at),
    duration_minutes = COALESCE(session_row.duration_minutes, 0)
FROM ranked_in_progress ranked
WHERE session_row.id = ranked.id
  AND ranked.row_rank > 1;

CREATE UNIQUE INDEX uq_surf_session_one_in_progress_per_user
    ON surf_session (user_id)
    WHERE status = 'IN_PROGRESS';
