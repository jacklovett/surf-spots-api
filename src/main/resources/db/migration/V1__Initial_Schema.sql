-- Initial database schema for Surf Spots application

-- Create continent table
CREATE TABLE continent (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(1000),
    slug VARCHAR(255)
);

-- Create country table
CREATE TABLE country (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(1000),
    slug VARCHAR(255),
    continent_id BIGINT,
    FOREIGN KEY (continent_id) REFERENCES continent(id) ON DELETE CASCADE
);

-- Create region table
CREATE TABLE region (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(1000),
    slug VARCHAR(255),
    country_id BIGINT,
    FOREIGN KEY (country_id) REFERENCES country(id) ON DELETE CASCADE
);

-- Create sub_region table
CREATE TABLE sub_region (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(1000),
    slug VARCHAR(255),
    region_id BIGINT,
    FOREIGN KEY (region_id) REFERENCES region(id) ON DELETE CASCADE
);

-- Create surf_spot table
CREATE TABLE surf_spot (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    beach_bottom_type VARCHAR(50),
    swell_direction VARCHAR(7),
    wind_direction VARCHAR(7),
    type VARCHAR(50),
    skill_level VARCHAR(50),
    tide VARCHAR(50),
    min_surf_height DOUBLE PRECISION,
    max_surf_height DOUBLE PRECISION,
    rating INTEGER CHECK (rating >= 0 AND rating <= 5),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    region_id BIGINT,
    sub_region_id BIGINT,
    status VARCHAR(50),
    food_nearby BOOLEAN,
    accommodation_nearby BOOLEAN,
    parking VARCHAR(50),
    boat_required BOOLEAN,
    season_start VARCHAR(9),
    season_end VARCHAR(9),
    created_by VARCHAR(255),
    created_at TIMESTAMP,
    modified_at TIMESTAMP,
    slug VARCHAR(255),
    FOREIGN KEY (region_id) REFERENCES region(id),
    FOREIGN KEY (sub_region_id) REFERENCES sub_region(id)
);

-- Create users table
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    name VARCHAR(255),
    password VARCHAR(255),
    country VARCHAR(255),
    city VARCHAR(255),
    settings_id BIGINT,
    created_at TIMESTAMP,
    modified_at TIMESTAMP
);

-- Create user_settings table
CREATE TABLE user_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    new_surf_spot_emails BOOLEAN DEFAULT false,
    nearby_surf_spots_emails BOOLEAN DEFAULT false,
    swell_season_emails BOOLEAN DEFAULT false,
    event_emails BOOLEAN DEFAULT false,
    promotion_emails BOOLEAN DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Add foreign key from users to user_settings
ALTER TABLE users ADD FOREIGN KEY (settings_id) REFERENCES user_settings(id);

-- Create user_auth_providers table
CREATE TABLE user_auth_providers (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create password_reset_token table
CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create user_surf_spot table
CREATE TABLE user_surf_spot (
    id BIGSERIAL PRIMARY KEY,
    is_favourite BOOLEAN DEFAULT false,
    created_at TIMESTAMP,
    modified_at TIMESTAMP,
    user_id VARCHAR(36),
    surf_spot_id BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (surf_spot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

-- Create watch_list_surf_spot table
CREATE TABLE watch_list_surf_spot (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36),
    surf_spot_id BIGINT,
    created_at TIMESTAMP,
    modified_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (surf_spot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

-- Create surfspot_food_options collection table
CREATE TABLE surfspot_food_options (
    surfspot_id BIGINT NOT NULL,
    food_option VARCHAR(50),
    FOREIGN KEY (surfspot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

-- Create surfspot_accommodation_options collection table
CREATE TABLE surfspot_accommodation_options (
    surfspot_id BIGINT NOT NULL,
    accommodation_option VARCHAR(50),
    FOREIGN KEY (surfspot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

-- Create surfspot_facilities collection table
CREATE TABLE surfspot_facilities (
    surfspot_id BIGINT NOT NULL,
    facility VARCHAR(50),
    FOREIGN KEY (surfspot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

-- Create surfspot_hazards collection table
CREATE TABLE surfspot_hazards (
    surfspot_id BIGINT NOT NULL,
    hazard VARCHAR(50),
    FOREIGN KEY (surfspot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

-- Create surf_spot_forecasts collection table
CREATE TABLE surf_spot_forecasts (
    surf_spot_id BIGINT NOT NULL,
    forecasts VARCHAR(255),
    FOREIGN KEY (surf_spot_id) REFERENCES surf_spot(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_country_continent ON country(continent_id);
CREATE INDEX idx_region_country ON region(country_id);
CREATE INDEX idx_subregion_region ON sub_region(region_id);
CREATE INDEX idx_surfspot_region ON surf_spot(region_id);
CREATE INDEX idx_surfspot_subregion ON surf_spot(sub_region_id);
CREATE INDEX idx_surfspot_status ON surf_spot(status);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_auth_provider_user ON user_auth_providers(user_id);
CREATE INDEX idx_password_reset_token ON password_reset_token(token);
CREATE INDEX idx_user_surf_spot_user ON user_surf_spot(user_id);
CREATE INDEX idx_user_surf_spot_spot ON user_surf_spot(surf_spot_id);
CREATE INDEX idx_watch_list_user ON watch_list_surf_spot(user_id);
CREATE INDEX idx_watch_list_spot ON watch_list_surf_spot(surf_spot_id);








