CREATE TABLE submission (
  id bigint NOT NULL PRIMARY KEY,
  version bigint NOT NULL,
  ega_box VARCHAR(255) NOT NULL,
  name_of_submission VARCHAR(255) NOT NULL,
  pub_med_id VARCHAR(255),
  state VARCHAR(255) NOT NULL,
  study_abstract text NOT NULL,
  study_name VARCHAR(255) NOT NULL,
  study_type VARCHAR(255) NOT NULL
);

CREATE TABLE bam_file_submission_object (
  id bigint NOT NULL PRIMARY KEY,
  version bigint NOT NULL,
  bam_file_id bigint NOT NULL,
  ega_alias_name VARCHAR(255),
  FOREIGN KEY (bam_file_id) REFERENCES abstract_bam_file(id)
);

CREATE TABLE data_file_submission_object (
  id bigint NOT NULL PRIMARY KEY,
  version bigint NOT NULL,
  data_file_id bigint NOT NULL,
  ega_alias_name VARCHAR(255),
  FOREIGN KEY (data_file_id) REFERENCES data_file(id)
);

CREATE TABLE sample_submission_object (
  id bigint NOT NULL PRIMARY KEY,
  version bigint NOT NULL,
  sample_id bigint NOT NULL,
  ega_alias_name VARCHAR(255),
  FOREIGN KEY (sample_id) REFERENCES sample(id)
);

CREATE TABLE submission_bam_file_submission_object (
  submission_bam_files_to_submit_id bigint NOT NULL REFERENCES submission(id),
  bam_file_submission_object_id bigint NOT NULL REFERENCES bam_file_submission_object(id),
  PRIMARY KEY (submission_bam_files_to_submit_id, bam_file_submission_object_id)
);

CREATE TABLE submission_data_file_submission_object (
  submission_data_files_to_submit_id bigint NOT NULL REFERENCES submission(id),
  data_file_submission_object_id bigint NOT NULL REFERENCES data_file_submission_object(id),
  PRIMARY KEY (submission_data_files_to_submit_id, data_file_submission_object_id)
);

CREATE TABLE submission_sample_submission_object (
  submission_samples_to_submit_id bigint NOT NULL REFERENCES submission(id),
  sample_submission_object_id bigint NOT NULL REFERENCES sample_submission_object(id),
  PRIMARY KEY (submission_samples_to_submit_id, sample_submission_object_id)
);
