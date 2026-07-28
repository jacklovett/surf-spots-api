-- Query performance indexes for hot paths identified in schema/repo audit.
-- IF NOT EXISTS so re-runs / partial local apply stay safe.

-- Session list / count / exists / in-progress lookup (partial uniques alone are not enough)
CREATE INDEX IF NOT EXISTS idx_surf_session_user_id ON surf_session (user_id);

-- Map bounds + nearby filters: status equality then lat/lon ranges
CREATE INDEX IF NOT EXISTS idx_surf_spot_status_lat_lon
    ON surf_spot (status, latitude, longitude);

-- OAuth login: findByProviderAndProviderId
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_auth_providers_provider_provider_id
    ON user_auth_providers (provider, provider_id);

-- Overdue live-session cron: status + share + expected return + not yet notified
CREATE INDEX IF NOT EXISTS idx_surf_session_overdue_notification
    ON surf_session (expected_return_instant)
    WHERE status = 'IN_PROGRESS'
      AND share_location_with_emergency_contact = TRUE
      AND overdue_notification_sent_at IS NULL
      AND expected_return_instant IS NOT NULL;

-- Region lookup by country + slug (e.g. south-west in multiple countries)
CREATE INDEX IF NOT EXISTS idx_region_country_slug ON region (country_id, slug);

-- Mapbox / name resolution (Spring Data IgnoreCase -> LOWER)
CREATE INDEX IF NOT EXISTS idx_country_lower_name ON country (LOWER(name));
CREATE INDEX IF NOT EXISTS idx_continent_lower_name ON continent (LOWER(name));

-- Contest season filters
CREATE INDEX IF NOT EXISTS idx_surf_event_contest_season_year
    ON surf_event_contest_detail (season_year);

-- Trip invitations sent by a user
CREATE INDEX IF NOT EXISTS idx_trip_invitation_invited_by ON trip_invitation (invited_by);
