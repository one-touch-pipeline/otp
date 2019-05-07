CREATE TABLE species_with_strain (
    id bigint NOT NULL PRIMARY KEY,
    version bigint NOT NULL,
    species_id bigint NOT NULL,
    strain_id bigint NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT species_with_strain_species_id_strain_id_key
    UNIQUE (species_id, strain_id)
);

CREATE TABLE strain (
    id bigint NOT NULL PRIMARY KEY,
    version bigint NOT NULL,
    name VARCHAR(255) NOT NULL UNIQUE,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE project
    DROP COLUMN species_id;

ALTER TABLE project
    ADD COLUMN species_with_strain_id bigint;

ALTER TABLE project
    ADD CONSTRAINT FK_project__species_with_strain_id FOREIGN KEY (species_with_strain_id) REFERENCES species_with_strain(id);


ALTER TABLE species_with_strain
    ADD CONSTRAINT FK_species_with_strain_id__species FOREIGN KEY (species_id) REFERENCES species(id);

ALTER TABLE species_with_strain
    ADD CONSTRAINT FK_species_with_strain_id__strain FOREIGN KEY (strain_id) REFERENCES strain(id);


ALTER TABLE project
    ADD COLUMN strain_id bigint;

CREATE TABLE common_name (
    id bigint NOT NULL PRIMARY KEY,
    version bigint NOT NULL,
    name VARCHAR(255) NOT NULL UNIQUE,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

-- change from String to Object Id
ALTER TABLE species
    DROP COLUMN common_name;

ALTER TABLE species
    ADD COLUMN common_name_id bigint;
