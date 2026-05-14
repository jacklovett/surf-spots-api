-- Optional stable id from an external system (wearable, partner API) for idempotent imports.
-- Sessions logged only through our UI omit this column.
-- Superseded by V34: uniqueness is (user_id, external_session_provider, external_session_id).
ALTER TABLE surf_session
    ADD COLUMN external_session_id VARCHAR(255);

CREATE UNIQUE INDEX uq_surf_session_user_external
    ON surf_session (user_id, external_session_id)
    WHERE external_session_id IS NOT NULL;
