ALTER TABLE project_request DROP COLUMN predecessor_project;
ALTER TABLE project_request ADD COLUMN related_projects VARCHAR(255);
ALTER TABLE project DROP COLUMN predecessor_project;
