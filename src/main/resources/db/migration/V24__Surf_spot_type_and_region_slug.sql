ALTER TABLE surf_spot
  DROP CONSTRAINT IF EXISTS surf_spot_type_check;

ALTER TABLE surf_spot
  ADD CONSTRAINT surf_spot_type_check
  CHECK (
    type IS NULL OR type IN (
      'BEACH_BREAK',
      'REEF_BREAK',
      'POINT_BREAK',
      'STANDING_WAVE'
    )
  );

WITH ranked AS (
  SELECT
    id,
    slug,
    region_id,
    ROW_NUMBER() OVER (
      PARTITION BY region_id, slug
      ORDER BY id
    ) AS rn
  FROM surf_spot
  WHERE slug IS NOT NULL
)
UPDATE surf_spot s
SET slug = s.slug || '-' || s.id
FROM ranked r
WHERE s.id = r.id
  AND r.rn > 1;

DROP INDEX IF EXISTS uq_surf_spot_region_slug_no_subregion;
DROP INDEX IF EXISTS uq_surf_spot_region_subregion_slug;

CREATE UNIQUE INDEX uq_surf_spot_region_slug
  ON surf_spot (region_id, slug)
  WHERE slug IS NOT NULL;
