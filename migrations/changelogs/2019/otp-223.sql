ALTER TABLE project DROP CONSTRAINT project_project_prefix_key;
ALTER TABLE project RENAME COLUMN project_prefix to individual_prefix;
ALTER TABLE project ADD COLUMN unique_individual_prefix BOOLEAN;
UPDATE project SET unique_individual_prefix = TRUE WHERE unique_individual_prefix ISNULL;
ALTER TABLE project ALTER COLUMN unique_individual_prefix SET NOT NULL;