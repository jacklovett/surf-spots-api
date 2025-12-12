-- Create trip_surfboard table to link trips with surfboards
CREATE TABLE trip_surfboard (
    id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    surfboard_id VARCHAR(36) NOT NULL,
    added_at TIMESTAMP NOT NULL,
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    FOREIGN KEY (surfboard_id) REFERENCES surfboards(id) ON DELETE CASCADE,
    UNIQUE(trip_id, surfboard_id)
);

-- Create index for better query performance
CREATE INDEX idx_trip_surfboard_trip ON trip_surfboard(trip_id);
CREATE INDEX idx_trip_surfboard_surfboard ON trip_surfboard(surfboard_id);



