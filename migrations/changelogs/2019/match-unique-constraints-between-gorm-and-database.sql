ALTER TABLE abstract_bam_file
  DROP CONSTRAINT unique_identifier;

ALTER TABLE abstract_bam_file
  DROP CONSTRAINT unique_work_directory_name;

ALTER TABLE pipeline
  DROP CONSTRAINT workflow_name_type_key;

ALTER TABLE pipeline
  ADD CONSTRAINT workflow_name_unique UNIQUE (name);