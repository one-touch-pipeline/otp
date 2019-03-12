CREATE TABLE SPECIES (
  id bigint NOT NULL PRIMARY KEY,
  version bigint NOT NULL,
  common_name VARCHAR(255) NOT NULL,
  scientific_name VARCHAR(255) UNIQUE NOT NULL
);

ALTER TABLE project
  ADD COLUMN species_id bigint;

ALTER TABLE project
 ADD CONSTRAINT FK_project__species_id FOREIGN KEY (species_id) REFERENCES species(id);
