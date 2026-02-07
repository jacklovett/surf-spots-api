-- Emergency services numbers per country (police, coastguard, ambulance, etc.)
CREATE TABLE country_emergency_number (
    id BIGSERIAL PRIMARY KEY,
    country_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    number VARCHAR(50) NOT NULL,
    CONSTRAINT fk_country_emergency_country FOREIGN KEY (country_id) REFERENCES country(id) ON DELETE CASCADE
);

CREATE INDEX idx_country_emergency_number_country_id ON country_emergency_number(country_id);
