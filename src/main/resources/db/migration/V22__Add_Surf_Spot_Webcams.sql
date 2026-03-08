-- Create surf_spot_webcams collection table (mirrors surf_spot_forecasts)
CREATE TABLE surf_spot_webcams (
    surf_spot_id BIGINT NOT NULL,
    webcams VARCHAR(255),
    FOREIGN KEY (surf_spot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_surf_spot_webcams_surf_spot ON surf_spot_webcams(surf_spot_id);
