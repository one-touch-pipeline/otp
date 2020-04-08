-- oldSystem flag for ClusterJob
ALTER TABLE cluster_job ADD COLUMN old_system BOOLEAN;
UPDATE cluster_job SET old_system = TRUE;
ALTER TABLE cluster_job ALTER COLUMN old_system SET NOT NULL;

-- priority for Workflow
-- values will be set at a later point in time
ALTER TABLE workflow ADD COLUMN priority SMALLINT;
UPDATE workflow SET priority = 0;
ALTER TABLE workflow ALTER COLUMN priority SET NOT NULL;