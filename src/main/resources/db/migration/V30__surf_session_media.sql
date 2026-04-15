CREATE TABLE surf_session_media (
    id VARCHAR(36) PRIMARY KEY,
    surf_session_id BIGINT NOT NULL REFERENCES surf_session (id) ON DELETE CASCADE,
    original_url TEXT NOT NULL,
    thumb_url TEXT,
    media_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_surf_session_media_session_id ON surf_session_media (surf_session_id);
