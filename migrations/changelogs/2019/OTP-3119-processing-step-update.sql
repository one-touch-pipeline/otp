ALTER TABLE processing_step_update
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE processing_step_update SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE processing_step_update ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE processing_step_update ALTER COLUMN last_updated SET NOT NULL;
