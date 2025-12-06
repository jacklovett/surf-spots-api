-- Create trip_invitation table
CREATE TABLE trip_invitation (
    id VARCHAR(36) PRIMARY KEY,
    trip_id VARCHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    invited_by VARCHAR(36) NOT NULL,
    invited_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    status VARCHAR(20) NOT NULL, -- PENDING, ACCEPTED, DECLINED, EXPIRED
    token VARCHAR(255),
    trip_member_id VARCHAR(36), -- Optional link to TripMember when accepted (for audit trail)
    FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE CASCADE,
    FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (trip_member_id) REFERENCES trip_member(id) ON DELETE SET NULL
);

-- Create unique partial index to prevent duplicate pending invitations for same email
-- This allows multiple invitations if previous ones were declined/expired
CREATE UNIQUE INDEX idx_trip_invitation_unique_pending 
    ON trip_invitation(trip_id, email) 
    WHERE status = 'PENDING';

-- Create indexes for better query performance
CREATE INDEX idx_trip_invitation_trip ON trip_invitation(trip_id);
CREATE INDEX idx_trip_invitation_email ON trip_invitation(email);
CREATE INDEX idx_trip_invitation_token ON trip_invitation(token);
CREATE INDEX idx_trip_invitation_status ON trip_invitation(status);

