CREATE TABLE ega_library_selection (
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    library_selection_ega_name VARCHAR(255) UNIQUE NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ega_library_selection_library_preparation_kit (
    ega_library_selection_id bigint NOT NULL REFERENCES ega_library_selection(id),
    library_preparation_kit_id bigint NOT NULL REFERENCES library_preparation_kit(id),
    PRIMARY KEY (ega_library_selection_id, library_preparation_kit_id)
);

CREATE TABLE ega_library_source (
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    library_source_ega_name VARCHAR(255) UNIQUE NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ega_library_source_seq_type (
    ega_library_source_id bigint NOT NULL REFERENCES ega_library_source(id),
    seq_type_id bigint NOT NULL REFERENCES seq_type(id),
    PRIMARY KEY (ega_library_source_id, seq_type_id)
);

CREATE TABLE ega_library_strategy (
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    library_strategy_ega_name VARCHAR(255) UNIQUE NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ega_library_strategy_seq_type (
    ega_library_strategy_id bigint NOT NULL REFERENCES ega_library_strategy(id),
    seq_type_id bigint NOT NULL REFERENCES seq_type(id),
    PRIMARY KEY (ega_library_strategy_id, seq_type_id)
);

CREATE TABLE ega_platform_model (
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    platform_model_ega_name VARCHAR(255) UNIQUE NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ega_platform_model_seq_platform_model_label (
    ega_platform_model_id bigint NOT NULL REFERENCES ega_platform_model(id),
    seq_platform_model_label_id bigint NOT NULL REFERENCES seq_platform_model_label(id),
    PRIMARY KEY (ega_platform_model_id, seq_platform_model_label_id)
);