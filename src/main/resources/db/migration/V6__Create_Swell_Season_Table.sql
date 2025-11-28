-- Create swell_season table
CREATE TABLE swell_season (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_month VARCHAR(9) NOT NULL,
    end_month VARCHAR(9) NOT NULL
);

-- Add swell_season_id to surf_spot table (if it doesn't already exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'surf_spot' AND column_name = 'swell_season_id') THEN
        ALTER TABLE surf_spot ADD COLUMN swell_season_id BIGINT;
    END IF;
END $$;

-- Add foreign key constraint (if it doesn't already exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_surf_spot_swell_season') THEN
        ALTER TABLE surf_spot ADD CONSTRAINT fk_surf_spot_swell_season 
            FOREIGN KEY (swell_season_id) REFERENCES swell_season(id);
    END IF;
END $$;

-- Create index for better query performance (if it doesn't already exist)
CREATE INDEX IF NOT EXISTS idx_surfspot_swell_season ON surf_spot(swell_season_id);

