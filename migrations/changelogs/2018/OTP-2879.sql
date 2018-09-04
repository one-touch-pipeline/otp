ALTER TABLE abstract_merging_work_package
ADD COLUMN config_id BIGINT;

ALTER TABLE abstract_merging_work_package
ADD FOREIGN KEY (config_id) REFERENCES config_per_project_and_seq_type(id);

ALTER TABLE abstract_merging_work_package
ADD COLUMN enforced_cells BIGINT;

ALTER TABLE abstract_merging_work_package
ADD COLUMN expected_cells BIGINT;
