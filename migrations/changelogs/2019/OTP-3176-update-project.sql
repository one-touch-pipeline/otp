CREATE TABLE keyword (
    id bigint NOT NULL PRIMARY KEY,
    version bigint NOT NULL,
    name VARCHAR(255) NOT NULL UNIQUE,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE project_keywords (
    project_id bigint NOT NULL REFERENCES project(id),
    keyword_id bigint NOT NULL REFERENCES keyword(id),
    PRIMARY KEY (project_id, keyword_id)
);

ALTER TABLE project ADD COLUMN end_date DATE;
ALTER TABLE project ADD COLUMN storage_until DATE;
ALTER TABLE project ADD COLUMN project_type VARCHAR(255);
ALTER TABLE project ADD COLUMN subsequent_application VARCHAR(255);
ALTER TABLE project ADD COLUMN connected_projects VARCHAR(255);
ALTER TABLE project ADD COLUMN internal_notes text;
ALTER TABLE project ADD COLUMN organisation_unit VARCHAR(255);

UPDATE project SET storage_until = '3000-01-01' WHERE storage_until ISNULL;
ALTER TABLE project ALTER COLUMN storage_until SET NOT NULL;

UPDATE project SET project_type = 'SEQUENCING' WHERE project_type ISNULL;
ALTER TABLE project ALTER COLUMN project_type SET NOT NULL;

ALTER TABLE project ALTER COLUMN unix_group SET NOT NULL;

DROP TABLE project_project_categories;
DROP TABLE project_category;
