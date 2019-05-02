ALTER TABLE meta_data_entry
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE meta_data_entry SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE meta_data_entry ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE meta_data_entry ALTER COLUMN last_updated SET NOT NULL;
