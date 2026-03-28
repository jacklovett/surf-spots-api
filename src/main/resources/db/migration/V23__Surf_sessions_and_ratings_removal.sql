ALTER TABLE surf_spot
    DROP COLUMN IF EXISTS rating;

CREATE TABLE surf_session (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    surf_spot_id BIGINT NOT NULL,
    skill_level VARCHAR(50) NOT NULL,
    session_date DATE NOT NULL,
    wave_size VARCHAR(50) NOT NULL,
    crowd_level VARCHAR(50) NOT NULL,
    wave_quality VARCHAR(50) NOT NULL,
    would_surf_again BOOLEAN NOT NULL,
    surfboard_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_surf_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_surf_session_spot FOREIGN KEY (surf_spot_id) REFERENCES surf_spot(id) ON DELETE CASCADE,
    CONSTRAINT fk_surf_session_surfboard FOREIGN KEY (surfboard_id) REFERENCES surfboards(id) ON DELETE SET NULL
);

CREATE INDEX idx_surf_session_spot ON surf_session(surf_spot_id);
CREATE INDEX idx_surf_session_skill_level ON surf_session(skill_level);
CREATE INDEX idx_surf_session_surfboard ON surf_session(surfboard_id);
