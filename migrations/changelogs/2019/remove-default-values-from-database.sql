ALTER TABLE seq_track
  ALTER COLUMN fastqc_state DROP DEFAULT;

ALTER TABLE run_segment
  ALTER COLUMN import_mode DROP DEFAULT;

ALTER TABLE abstract_bam_file
  ALTER COLUMN quality_assessment_status DROP DEFAULT;

ALTER TABLE seq_track
  ALTER COLUMN quality_encoding DROP DEFAULT;

ALTER TABLE project
  ALTER COLUMN snv DROP DEFAULT;

ALTER TABLE abstract_bam_file
  ALTER COLUMN has_coverage_plot DROP DEFAULT;

ALTER TABLE abstract_bam_file
  ALTER COLUMN has_insert_size_plot DROP DEFAULT;

ALTER TABLE abstract_bam_file
  ALTER COLUMN withdrawn DROP DEFAULT;

ALTER TABLE project
  ALTER COLUMN processing_priority DROP DEFAULT;