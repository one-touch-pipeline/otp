ALTER TABLE project RENAME COLUMN connected_projects TO related_projects;
ALTER TABLE project RENAME COLUMN organisation_unit TO organizational_unit;
ALTER TABLE project RENAME COLUMN subsequent_application TO predecessor_project;
