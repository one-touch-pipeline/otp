ALTER TABLE sample_submission_object ADD COLUMN use_bam_file BOOLEAN NOT NULL;
ALTER TABLE sample_submission_object ADD COLUMN use_fastq_file BOOLEAN NOT NULL;
ALTER TABLE sample_submission_object ADD CONSTRAINT ega_alias_name_key UNIQUE (ega_alias_name);
