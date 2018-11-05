ALTER TABLE data_file_submission_object ADD COLUMN sample_submission_object_id bigint NOT NULL;
ALTER TABLE bam_file_submission_object ADD COLUMN sample_submission_object_id bigint NOT NULL;
ALTER TABLE data_file_submission_object ADD FOREIGN KEY (sample_submission_object_id) REFERENCES sample_submission_object(id);
ALTER TABLE bam_file_submission_object ADD FOREIGN KEY (sample_submission_object_id) REFERENCES sample_submission_object(id);
ALTER TABLE submission ADD column selection_state VARCHAR (255) NOT NULL;
ALTER TABLE data_file_submission_object ADD CONSTRAINT data_file_ega_alias_name_key UNIQUE (ega_alias_name);
ALTER TABLE bam_file_submission_object ADD CONSTRAINT bam_file_ega_alias_name_key UNIQUE (ega_alias_name);
