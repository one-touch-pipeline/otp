CREATE TABLE workflow_error(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    message TEXT NOT NULL,
    stacktrace TEXT NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

-- abstract super class
CREATE TABLE workflow_log(
    id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL,
    date_created TIMESTAMP WITH TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

-- child classes
CREATE TABLE workflow_message_log(
    id BIGINT NOT NULL PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES workflow_log(id),
    message TEXT NOT NULL
);

CREATE TABLE workflow_command_log(
    id BIGINT NOT NULL PRIMARY KEY,
    FOREIGN KEY (id) REFERENCES workflow_log(id),
    command TEXT NOT NULL,
    exit_code BIGINT NOT NULL,
    stdout TEXT NOT NULL,
    stderr TEXT NOT NULL
);