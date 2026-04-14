-- Normalize free-text relationship values to enum constant names before JPA maps the column to EmergencyContactRelationship.
UPDATE users
SET emergency_contact_relationship = CASE LOWER(TRIM(emergency_contact_relationship))
    WHEN 'parent' THEN 'PARENT'
    WHEN 'mother' THEN 'PARENT'
    WHEN 'father' THEN 'PARENT'
    WHEN 'guardian' THEN 'PARENT'
    WHEN 'spouse' THEN 'SPOUSE'
    WHEN 'wife' THEN 'SPOUSE'
    WHEN 'husband' THEN 'SPOUSE'
    WHEN 'partner' THEN 'PARTNER'
    WHEN 'sibling' THEN 'SIBLING'
    WHEN 'brother' THEN 'SIBLING'
    WHEN 'sister' THEN 'SIBLING'
    WHEN 'child' THEN 'CHILD'
    WHEN 'son' THEN 'CHILD'
    WHEN 'daughter' THEN 'CHILD'
    WHEN 'friend' THEN 'FRIEND'
    WHEN 'other' THEN 'OTHER'
    ELSE emergency_contact_relationship
END
WHERE emergency_contact_relationship IS NOT NULL;

-- Drop values that are not valid enum names (unmappable legacy text).
UPDATE users
SET emergency_contact_relationship = NULL
WHERE emergency_contact_relationship IS NOT NULL
  AND emergency_contact_relationship NOT IN (
    'PARENT', 'SPOUSE', 'PARTNER', 'SIBLING', 'CHILD', 'FRIEND', 'OTHER'
  );
