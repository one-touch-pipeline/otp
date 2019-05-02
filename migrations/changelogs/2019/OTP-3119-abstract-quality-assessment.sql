ALTER TABLE abstract_quality_assessment
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE abstract_quality_assessment SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE abstract_quality_assessment ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE abstract_quality_assessment ALTER COLUMN last_updated SET NOT NULL;
