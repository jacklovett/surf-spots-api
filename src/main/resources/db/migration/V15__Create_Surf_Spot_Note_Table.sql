-- Create surf_spot_note table
CREATE TABLE surf_spot_note (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    surf_spot_id BIGINT NOT NULL,
    note_text VARCHAR(10000),
    preferred_tide VARCHAR(50),
    preferred_wind VARCHAR(100),
    preferred_swell_direction VARCHAR(100),
    preferred_swell_range VARCHAR(100),
    skill_requirement VARCHAR(50),
    created_at TIMESTAMP,
    modified_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (surf_spot_id) REFERENCES surf_spot(id) ON DELETE CASCADE,
    UNIQUE (user_id, surf_spot_id)
);

-- Create index for faster lookups
CREATE INDEX idx_surf_spot_note_user_spot ON surf_spot_note(user_id, surf_spot_id);

