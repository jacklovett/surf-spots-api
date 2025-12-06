-- Create trip table
CREATE TABLE trip (
    id VARCHAR(36) PRIMARY KEY,
    owner_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create trip_member table
CREATE TABLE trip_member (
    id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    added_at TIMESTAMP NOT NULL,
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(trip_id, user_id)
);

-- Create trip_spot table
CREATE TABLE trip_spot (
    id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    surf_spot_id BIGINT NOT NULL,
    added_at TIMESTAMP NOT NULL,
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    FOREIGN KEY (surf_spot_id) REFERENCES surf_spot(id) ON DELETE CASCADE,
    UNIQUE(trip_id, surf_spot_id)
);

-- Create trip_media table
CREATE TABLE trip_media (
    id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    url TEXT NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_trip_owner ON trip(owner_id);
CREATE INDEX idx_trip_member_trip ON trip_member(trip_id);
CREATE INDEX idx_trip_member_user ON trip_member(user_id);
CREATE INDEX idx_trip_spot_trip ON trip_spot(trip_id);
CREATE INDEX idx_trip_spot_surf_spot ON trip_spot(surf_spot_id);
CREATE INDEX idx_trip_media_trip ON trip_media(trip_id);
CREATE INDEX idx_trip_media_owner ON trip_media(owner_id);







