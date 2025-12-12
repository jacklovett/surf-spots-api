-- Create surfboards table
CREATE TABLE surfboards (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    board_type VARCHAR(50),
    length DECIMAL(5,2),
    width DECIMAL(5,2),
    thickness DECIMAL(5,2),
    volume DECIMAL(6,2),
    fin_setup VARCHAR(50),
    description TEXT,
    model_url TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create surfboard_images table
CREATE TABLE surfboard_images (
    id VARCHAR(36) PRIMARY KEY,
    surfboard_id VARCHAR(36) NOT NULL,
    original_url TEXT NOT NULL,
    thumb_url TEXT,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (surfboard_id) REFERENCES surfboards(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_surfboards_user ON surfboards(user_id);
CREATE INDEX idx_surfboard_images_surfboard ON surfboard_images(surfboard_id);



