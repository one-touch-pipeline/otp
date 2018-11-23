-- new column for enum QcThresholdHandling
ALTER TABLE project
  ADD COLUMN qc_threshold_handling VARCHAR(255);

-- the default behaviour until now was CHECK_NOTIFY_AND_BLOCK, so it will be used as the default
UPDATE project
  SET qc_threshold_handling = 'CHECK_NOTIFY_AND_BLOCK';

-- new column for enum QcThresholdHandling
ALTER TABLE project
  ALTER COLUMN qc_threshold_handling SET NOT NULL;
