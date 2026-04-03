-- Optional emergency contact on user profile (name, phone, relationship)
ALTER TABLE users ADD COLUMN emergency_contact_name VARCHAR(255);
ALTER TABLE users ADD COLUMN emergency_contact_phone VARCHAR(50);
ALTER TABLE users ADD COLUMN emergency_contact_relationship VARCHAR(100);
