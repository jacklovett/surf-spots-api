-- Idempotency key is per user + integration source + provider-local id (same raw id allowed across providers).
ALTER TABLE surf_session
    ADD COLUMN external_session_provider VARCHAR(64);

-- Only experimental rows could have external_session_id without a provider (never shipped). Drop the id so the
-- pair CHECK succeeds; the surf_session row itself is kept.
UPDATE surf_session
SET external_session_id = NULL
WHERE external_session_id IS NOT NULL;

DROP INDEX IF EXISTS uq_surf_session_user_external;

ALTER TABLE surf_session
    ADD CONSTRAINT chk_surf_session_external_pair CHECK (
        (external_session_id IS NULL AND external_session_provider IS NULL)
        OR (external_session_id IS NOT NULL AND external_session_provider IS NOT NULL)
    );

CREATE UNIQUE INDEX uq_surf_session_user_provider_external
    ON surf_session (user_id, external_session_provider, external_session_id)
    WHERE external_session_id IS NOT NULL AND external_session_provider IS NOT NULL;
