CREATE TABLE wes_server(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE workflow_step(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE workflow_config(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE process_parameter
RENAME TO artefact;

CREATE TABLE skipped_message(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(255) NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE workflow(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    bean_name VARCHAR(255),
    enabled BOOLEAN NOT NULL,
    deprecated_date TIMESTAMP WITH TIME ZONE,
    wes_server_id BIGINT,
    max_parallel_running BIGINT NOT NULL,
    comment_id BIGINT,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (wes_server_id) REFERENCES wes_server(id),
    FOREIGN KEY (comment_id) REFERENCES comment(id)
);

CREATE INDEX workflow_wes_server_idx on workflow(wes_server_id);
CREATE INDEX workflow_comment_idx on workflow(comment_id);

    CREATE TABLE workflow_run(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    work_directory VARCHAR(255),
    combined_config text,
    priority SMALLINT NOT NULL,
    state VARCHAR(255) NOT NULL,
    restarted_from_id BIGINT,
    skipped_message_id BIGINT,
    workflow_id BIGINT NOT NULL,
    comment_id BIGINT,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (restarted_from_id) REFERENCES workflow_run(id),
    FOREIGN KEY (skipped_message_id) REFERENCES skipped_message(id),
    FOREIGN KEY (workflow_id) REFERENCES workflow(id),
    FOREIGN KEY (comment_id) REFERENCES comment(id)
);
CREATE INDEX workflow_run_restarted_from_idx on workflow_run(restarted_from_id);
CREATE INDEX workflow_run_skipped_message_idx on workflow_run(skipped_message_id);
CREATE INDEX workflow_run_workflow_idx on workflow_run(workflow_id);
CREATE INDEX workflow_run_comment_idx on workflow_run(comment_id);

CREATE TABLE workflow_artefact(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    produced_by_id BIGINT,
    artefact_id BIGINT,
    state VARCHAR(255) NOT NULL,
    withdrawn_date TIMESTAMP WITH TIME ZONE,
    withdrawn_comment TEXT,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (produced_by_id) REFERENCES workflow_run(id),
    FOREIGN KEY (artefact_id) REFERENCES artefact(id)
);

CREATE INDEX workflow_artefact_produced_by_idx on workflow_artefact(produced_by_id);
CREATE INDEX workflow_artefact_artefact_idx on workflow_artefact(artefact_id);


CREATE TABLE workflow_run_input_artefacts(
    input_artefacts_id BIGINT NOT NULL,
    produced_by_id BIGINT NOT NULL,
    input_artefacts_idx VARCHAR(255) NOT NULL,
    FOREIGN KEY (input_artefacts_id) REFERENCES workflow_run(id),
    FOREIGN KEY (produced_by_id) REFERENCES workflow_artefact(id)
);

CREATE INDEX workflow_run_input_artefacts_input_artefacts_idx on workflow_run_input_artefacts(input_artefacts_id);
CREATE INDEX workflow_run_input_artefacts_produced_by_idx on workflow_run_input_artefacts(produced_by_id);

CREATE TABLE workflow_run_output_artefacts(
    output_artefacts_id BIGINT NOT NULL,
    produced_by_id BIGINT NOT NULL,
    output_artefacts_idx VARCHAR(255) NOT NULL,
    FOREIGN KEY (output_artefacts_id) REFERENCES workflow_run(id),
    FOREIGN KEY (produced_by_id) REFERENCES workflow_artefact(id)
);

CREATE INDEX workflow_run_output_artefacts_output_artefacts_idx on workflow_run_output_artefacts(output_artefacts_id);
CREATE INDEX workflow_run_output_artefacts_produced_by_idx on workflow_run_output_artefacts(produced_by_id);

CREATE TABLE workflow_run_workflow_config(
    workflow_run_configs_id BIGINT NOT NULL,
    workflow_config_id BIGINT NOT NULL,
    configs_idx INT NOT NULL,
    FOREIGN KEY (workflow_config_id) REFERENCES workflow_config(id)
);

CREATE INDEX workflow_run_workflow_config_workflow_config_idx on workflow_run_workflow_config(workflow_config_id);

CREATE TABLE workflow_run_workflow_step(
    workflow_run_workflow_steps_id BIGINT NOT NULL,
    workflow_step_id BIGINT NOT NULL,
    workflow_steps_idx INT NOT NULL,
    FOREIGN KEY (workflow_step_id) REFERENCES workflow_step(id)
);

CREATE INDEX workflow_run_workflow_step_workflow_step_idx on workflow_run_workflow_step(workflow_step_id);