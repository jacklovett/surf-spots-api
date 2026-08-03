-- Normalised environmental alerts for surf spots (watch-list notifications).
CREATE TABLE environmental_alert (
    id              BIGSERIAL PRIMARY KEY,
    surf_spot_id    BIGINT NOT NULL REFERENCES surf_spot(id) ON DELETE CASCADE,
    type            VARCHAR(64) NOT NULL,
    severity        VARCHAR(32) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    source_name     VARCHAR(255) NOT NULL,
    source_url      VARCHAR(1000),
    external_id     VARCHAR(255) NOT NULL,
    detected_at     TIMESTAMPTZ NOT NULL,
    expires_at      TIMESTAMPTZ,
    status          VARCHAR(32) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT environmental_alert_type_check
        CHECK (type IN ('WATER_QUALITY_ADVISORY', 'SEWAGE_OVERFLOW', 'BEACH_ACCESS_CLOSED')),
    CONSTRAINT environmental_alert_severity_check
        CHECK (severity IN ('CAUTION', 'WARNING')),
    CONSTRAINT environmental_alert_status_check
        CHECK (status IN ('ACTIVE', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_environmental_alert_active_dedupe
    ON environmental_alert (surf_spot_id, type, external_id)
    WHERE status = 'ACTIVE';

CREATE INDEX idx_environmental_alert_surf_spot_id ON environmental_alert (surf_spot_id);
CREATE INDEX idx_environmental_alert_status ON environmental_alert (status);
CREATE INDEX idx_environmental_alert_type ON environmental_alert (type);
CREATE INDEX idx_environmental_alert_external_id ON environmental_alert (external_id);
CREATE INDEX idx_environmental_alert_surf_spot_status ON environmental_alert (surf_spot_id, status);
