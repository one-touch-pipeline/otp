ALTER TABLE submission ADD COLUMN project_id bigint NOT NULL REFERENCES project(id);
ALTER TABLE submission RENAME COLUMN name_of_submission TO submission_name;
