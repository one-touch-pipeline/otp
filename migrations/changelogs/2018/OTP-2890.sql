ALTER TABLE config_per_project_and_seq_type
ADD COLUMN reference_genome_index_id bigint REFERENCES reference_genome_index(id);

UPDATE tool_name set name = 'CELL_RANGER' where name = 'cellranger';
UPDATE tool_name set type = 'SINGLE_CELL' where type = 'CELLRANGER';
