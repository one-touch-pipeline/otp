-- rename the table of the run segment domain
ALTER TABLE run_segment
  RENAME TO fastq_import_instance;

-- rename columns referencing run segment
ALTER TABLE data_file
  RENAME run_segment_id TO fastq_import_instance_id;

ALTER TABLE meta_data_file
  RENAME run_segment_id TO fastq_import_instance_id;

-- drop old indexes
DROP INDEX run_segment_otrs_ticket_idx;

DROP INDEX meta_data_file_run_segment_idx;
DROP INDEX data_file_run_segment_idx;

-- recreate indexes
CREATE INDEX fastq_import_instance_otrs_ticket_idx
  ON fastq_import_instance(otrs_ticket_id);

CREATE INDEX meta_data_file_fastq_import_instance_idx
  ON meta_data_file(fastq_import_instance_id);
CREATE INDEX data_file_fastq_import_instance_idx
  ON data_file(fastq_import_instance_id);
