
UPDATE abstract_bam_file SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE abstract_bam_file ALTER COLUMN date_created SET NOT NULL;


UPDATE bam_file_pair_analysis SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE bam_file_pair_analysis ALTER COLUMN date_created SET NOT NULL;

UPDATE bam_file_pair_analysis SET last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE last_updated IS NULL;
ALTER TABLE bam_file_pair_analysis ALTER COLUMN last_updated SET NOT NULL;


UPDATE change_log SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE change_log ALTER COLUMN date_created SET NOT NULL;


UPDATE config_per_project_and_seq_type SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE config_per_project_and_seq_type ALTER COLUMN date_created SET NOT NULL;

UPDATE config_per_project_and_seq_type SET last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE last_updated IS NULL;
ALTER TABLE config_per_project_and_seq_type ALTER COLUMN last_updated SET NOT NULL;


UPDATE data_file SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE data_file ALTER COLUMN date_created SET NOT NULL;


UPDATE fastqc_processed_file SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE fastqc_processed_file ALTER COLUMN date_created SET NOT NULL;


UPDATE gene_model SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE gene_model ALTER COLUMN date_created SET NOT NULL;

UPDATE gene_model SET last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE last_updated IS NULL;
ALTER TABLE gene_model ALTER COLUMN last_updated SET NOT NULL;


UPDATE log_message SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE log_message ALTER COLUMN date_created SET NOT NULL;


UPDATE meta_data_file SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE meta_data_file ALTER COLUMN date_created SET NOT NULL;


UPDATE otrs_ticket SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE otrs_ticket ALTER COLUMN date_created SET NOT NULL;


UPDATE processed_sai_file SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE processed_sai_file ALTER COLUMN date_created SET NOT NULL;


UPDATE processing_option SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE processing_option ALTER COLUMN date_created SET NOT NULL;


UPDATE processing_thresholds SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE processing_thresholds ALTER COLUMN date_created SET NOT NULL;

UPDATE processing_thresholds SET last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE last_updated IS NULL;
ALTER TABLE processing_thresholds ALTER COLUMN last_updated SET NOT NULL;


UPDATE project_info SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE project_info ALTER COLUMN date_created SET NOT NULL;


UPDATE reference_genome_project_seq_type SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE reference_genome_project_seq_type ALTER COLUMN date_created SET NOT NULL;


UPDATE run SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE run ALTER COLUMN date_created SET NOT NULL;


UPDATE sample_pair SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE sample_pair ALTER COLUMN date_created SET NOT NULL;

UPDATE sample_pair SET last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE last_updated IS NULL;
ALTER TABLE sample_pair ALTER COLUMN last_updated SET NOT NULL;


UPDATE sample_type_per_project SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE sample_type_per_project ALTER COLUMN date_created SET NOT NULL;

UPDATE sample_type_per_project SET last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE last_updated IS NULL;
ALTER TABLE sample_type_per_project ALTER COLUMN last_updated SET NOT NULL;


UPDATE seq_scan SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE seq_scan ALTER COLUMN date_created SET NOT NULL;


UPDATE swap_info SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE swap_info ALTER COLUMN date_created SET NOT NULL;


UPDATE users SET date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE date_created IS NULL;
ALTER TABLE users ALTER COLUMN date_created SET NOT NULL;

UPDATE users SET last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00' WHERE last_updated IS NULL;
ALTER TABLE users ALTER COLUMN last_updated SET NOT NULL;
