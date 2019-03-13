ALTER TABLE job_definition_parameter
  ALTER COLUMN job_definition_constant_parameters_id SET NOT NULL;

ALTER TABLE cluster_job_cluster_job
  ALTER COLUMN cluster_job_dependencies_id SET NOT NULL;
