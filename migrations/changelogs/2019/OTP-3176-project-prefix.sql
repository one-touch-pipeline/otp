ALTER TABLE project ADD COLUMN project_prefix VARCHAR(255) UNIQUE;
/* NOTE: set this after release */
UPDATE project SET project_prefix = name WHERE project_prefix ISNULL;
ALTER TABLE project ALTER COLUMN project_prefix SET NOT NULL;

ALTER TABLE project DROP COLUMN phabricator_alias;
