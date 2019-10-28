-- new reference genome index field for the merging work package
ALTER TABLE abstract_merging_work_package
  ADD COLUMN reference_genome_index_id BIGINT;

ALTER TABLE abstract_merging_work_package
  ADD FOREIGN KEY (reference_genome_index_id)
  REFERENCES reference_genome_index(id);

-- for existing work packages, copy the reference genome index of the used config
UPDATE abstract_merging_work_package
  SET
    reference_genome_index_id = config_per_project_and_seq_type.reference_genome_index_id
  FROM
    config_per_project_and_seq_type
  WHERE
    config_id = config_per_project_and_seq_type.id;

-- remove the reference genome index from the config
ALTER TABLE config_per_project_and_seq_type
  DROP COLUMN reference_genome_index_id;
