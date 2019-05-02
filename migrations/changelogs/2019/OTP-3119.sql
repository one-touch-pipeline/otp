ALTER TABLE abstract_bam_file
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE abstract_bam_file SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE abstract_bam_file ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE abstract_merging_work_package
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE abstract_merging_work_package SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE abstract_merging_work_package ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE abstract_merging_work_package ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE aceseq_qc
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE aceseq_qc SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE aceseq_qc ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE aceseq_qc ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE alignment_log
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE alignment_log SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE alignment_log ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE alignment_log ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE alignment_params
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE alignment_params SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE alignment_params ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE alignment_params ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE alignment_pass
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE alignment_pass SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE alignment_pass ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE alignment_pass ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE antibody_target
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE antibody_target SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE antibody_target ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE antibody_target ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE audit_log
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE audit_log SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE audit_log ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE audit_log ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE bam_file_submission_object
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE bam_file_submission_object SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE bam_file_submission_object ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE bam_file_submission_object ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE bed_file
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE bed_file SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE bed_file ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE bed_file ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE change_log
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE change_log SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE change_log ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE comment
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE comment SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE comment ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE comment ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE consistency_check
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE consistency_check SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE consistency_check ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE consistency_check ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE consistency_status
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE consistency_status SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE consistency_status ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE consistency_status ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE data_file
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE data_file SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE data_file ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE data_file_submission_object
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE data_file_submission_object SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE data_file_submission_object ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE data_file_submission_object ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE decision_mapping
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE decision_mapping SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE decision_mapping ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE decision_mapping ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE document
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE document SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE document ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE document ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE document_type
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE document_type SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE document_type ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE document_type ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE ega_submission
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE ega_submission SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE ega_submission ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE ega_submission ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE fastqc_processed_file
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE fastqc_processed_file SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE fastqc_processed_file ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE file_system_changes
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE file_system_changes SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE file_system_changes ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE file_system_changes ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE file_type
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE file_type SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE file_type ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE file_type ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE groups
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE groups SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE groups ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE groups ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE ilse_submission
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE ilse_submission SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE ilse_submission ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE ilse_submission ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE import_process
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE import_process SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE import_process ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE import_process ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE indel_quality_control
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE indel_quality_control SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE indel_quality_control ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE indel_quality_control ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE indel_sample_swap_detection
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE indel_sample_swap_detection SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE indel_sample_swap_detection ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE indel_sample_swap_detection ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE individual
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE individual SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE individual ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE individual ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE job_decision
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE job_decision SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE job_decision ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE job_decision ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE job_definition
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE job_definition SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE job_definition ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE job_definition ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE job_error_definition
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE job_error_definition SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE job_error_definition ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE job_error_definition ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE job_execution_plan
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE job_execution_plan SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE job_execution_plan ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE job_execution_plan ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE library_preparation_kit
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE library_preparation_kit SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE library_preparation_kit ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE library_preparation_kit ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE log_message
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE log_message SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE log_message ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merged_alignment_data_file
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merged_alignment_data_file SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merged_alignment_data_file ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merged_alignment_data_file ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merging_assignment
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merging_assignment SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merging_assignment ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merging_assignment ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merging_criteria
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merging_criteria SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merging_criteria ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merging_criteria ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merging_log
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merging_log SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merging_log ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merging_log ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merging_pass
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merging_pass SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merging_pass ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merging_pass ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merging_set
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merging_set SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merging_set ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merging_set ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merging_set_assignment
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merging_set_assignment SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merging_set_assignment ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merging_set_assignment ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE merging_work_package_alignment_property
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE merging_work_package_alignment_property SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE merging_work_package_alignment_property ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE merging_work_package_alignment_property ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE meta_data_file
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE meta_data_file SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE meta_data_file ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE meta_data_key
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE meta_data_key SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE meta_data_key ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE meta_data_key ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE otrs_ticket
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE otrs_ticket SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE otrs_ticket ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE parameter_mapping
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE parameter_mapping SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE parameter_mapping ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE parameter_mapping ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE parameter_type
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE parameter_type SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE parameter_type ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE parameter_type ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE picard_mark_duplicates_metrics
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE picard_mark_duplicates_metrics SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE picard_mark_duplicates_metrics ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE picard_mark_duplicates_metrics ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE pipeline
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE pipeline SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE pipeline ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE pipeline ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE process
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE process SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE process ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE process ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE process_parameter
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE process_parameter SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE process_parameter ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE process_parameter ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE processed_sai_file
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE processed_sai_file SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE processed_sai_file ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE processing_error
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE processing_error SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE processing_error ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE processing_error ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE processing_option
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE processing_option SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE processing_option ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE project
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE project SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE project ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE project ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE project_category
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE project_category SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE project_category ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE project_category ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE project_group
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE project_group SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE project_group ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE project_group ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE project_info
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE project_info SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE project_info ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE project_role
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE project_role SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE project_role ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE project_role ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE qc_threshold
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE qc_threshold SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE qc_threshold ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE qc_threshold ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE quality_assessment_merged_pass
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE quality_assessment_merged_pass SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE quality_assessment_merged_pass ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE quality_assessment_merged_pass ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE quality_assessment_pass
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE quality_assessment_pass SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE quality_assessment_pass ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE quality_assessment_pass ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE realm
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE realm SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE realm ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE realm ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE reference_genome
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE reference_genome SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE reference_genome ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE reference_genome ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE reference_genome_entry
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE reference_genome_entry SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE reference_genome_entry ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE reference_genome_entry ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE reference_genome_index
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE reference_genome_index SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE reference_genome_index ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE reference_genome_index ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE reference_genome_project_seq_type
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE reference_genome_project_seq_type SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE reference_genome_project_seq_type ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE reference_genome_project_seq_type_alignment_property
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE reference_genome_project_seq_type_alignment_property SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE reference_genome_project_seq_type_alignment_property ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE reference_genome_project_seq_type_alignment_property ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE referenced_class
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE referenced_class SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE referenced_class ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE referenced_class ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE role
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE role SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE role ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE role ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE run
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE run SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE run ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE run_segment
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE run_segment SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE run_segment ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE run_segment ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE sample
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE sample SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE sample ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE sample ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE sample_identifier
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE sample_identifier SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE sample_identifier ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE sample_identifier ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE sample_submission_object
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE sample_submission_object SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE sample_submission_object ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE sample_submission_object ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE sample_type
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE sample_type SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE sample_type ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE sample_type ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE seq_center
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE seq_center SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE seq_center ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE seq_center ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE seq_platform
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE seq_platform SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE seq_platform ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE seq_platform ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE seq_platform_group
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE seq_platform_group SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE seq_platform_group ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE seq_platform_group ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE seq_platform_model_label
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE seq_platform_model_label SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE seq_platform_model_label ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE seq_platform_model_label ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE seq_scan
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE seq_scan SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE seq_scan ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE seq_track
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE seq_track SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE seq_track ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE seq_track ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE seq_type
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE seq_type SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE seq_type ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE seq_type ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE sequencing_kit_label
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE sequencing_kit_label SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE sequencing_kit_label ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE sequencing_kit_label ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE shutdown_information
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE shutdown_information SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE shutdown_information ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE shutdown_information ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE software_tool
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE software_tool SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE software_tool ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE software_tool ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE software_tool_identifier
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE software_tool_identifier SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE software_tool_identifier ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE software_tool_identifier ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE sophia_qc
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE sophia_qc SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE sophia_qc ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE sophia_qc ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE species
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE species SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE species ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE species ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE stat_size_file_name
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE stat_size_file_name SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE stat_size_file_name ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE stat_size_file_name ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE swap_info
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE swap_info SET
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE swap_info ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE tool_name
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE tool_name SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE tool_name ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE tool_name ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE tumor_entity
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE tumor_entity SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE tumor_entity ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE tumor_entity ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE user_project_role
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE user_project_role SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE user_project_role ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE user_project_role ALTER COLUMN last_updated SET NOT NULL;


ALTER TABLE user_role
  ADD COLUMN date_created TIMESTAMP WITH TIME ZONE,
  ADD COLUMN last_updated TIMESTAMP WITH TIME ZONE;

UPDATE user_role SET
  date_created = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00',
  last_updated = TIMESTAMP WITH TIME ZONE '1970-01-01 00:00:00+00';

ALTER TABLE user_role ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE user_role ALTER COLUMN last_updated SET NOT NULL;
