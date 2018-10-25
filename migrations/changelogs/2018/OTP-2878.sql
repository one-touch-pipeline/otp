CREATE TABLE document_type (
  id bigint NOT NULL PRIMARY KEY,
  version bigint NOT NULL,
  description text NOT NULL,
  title VARCHAR(255) UNIQUE NOT NULL
);

INSERT INTO document_type (id, version, title, description)
VALUES (nextval('hibernate_sequence'), 0, 'PROJECT_FORM', 'If you want to set up a new project in OTP, please fill in this project form. It has to be signed by the project''s PI.');

INSERT INTO document_type (id, version, title, description)
VALUES (nextval('hibernate_sequence'), 0, 'METADATA_TEMPLATE', 'To provide the corresponding metadata to your project’s samples please use this template.');

INSERT INTO document_type (id, version, title, description)
VALUES (nextval('hibernate_sequence'), 0, 'PROCESSING_INFORMATION', 'We need this processing information to know the processing steps you want to be applied on you samples.');

ALTER TABLE document ADD COLUMN document_type_id bigint;
update document set document_type_id = (select id from document_type where title = document.name);

DROP INDEX document__name_idx;

ALTER TABLE document DROP COLUMN name;
ALTER TABLE document
 ADD CONSTRAINT FK_document__document_type_id FOREIGN KEY (document_type_id) REFERENCES document_type(id);
ALTER TABLE document ALTER COLUMN document_type_id SET NOT NULL;

ALTER TABLE document RENAME COLUMN type TO format_type;

CREATE INDEX document_type_title_idx ON document_type (title);
CREATE INDEX document__document_type_idx ON document (document_type_id);

