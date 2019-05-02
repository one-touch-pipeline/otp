ALTER TABLE processing_step
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE processing_step SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE processing_step ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE processing_step ALTER COLUMN last_updated SET NOT NULL;
