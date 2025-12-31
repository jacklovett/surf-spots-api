-- Add age, gender, skill_level, height, and weight columns to users table
ALTER TABLE users ADD COLUMN age INTEGER;
ALTER TABLE users ADD COLUMN gender VARCHAR(50);
ALTER TABLE users ADD COLUMN skill_level VARCHAR(50);
ALTER TABLE users ADD COLUMN height INTEGER;
ALTER TABLE users ADD COLUMN weight INTEGER;

-- Add CHECK constraints for validation
-- Age: 13-120 years (matches frontend validation)
-- Height: 50-305 cm (50 cm = metric min, 305 cm = 120 inches after conversion)
-- Weight: 9-500 kg (9 kg = 20 lbs after conversion, 500 kg = metric max)
ALTER TABLE users ADD CONSTRAINT check_age_range CHECK (age IS NULL OR (age >= 13 AND age <= 120));
ALTER TABLE users ADD CONSTRAINT check_height_range CHECK (height IS NULL OR (height >= 50 AND height <= 305));
ALTER TABLE users ADD CONSTRAINT check_weight_range CHECK (weight IS NULL OR (weight >= 9 AND weight <= 500));