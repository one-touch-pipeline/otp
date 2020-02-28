-- previously used for CellRanger and RunYapsa configs
UPDATE
    config_per_project_and_seq_type
SET
    program_version = external_script_version
WHERE
    program_version IS NULL AND
    external_script_version IS NOT NULL;


ALTER TABLE config_per_project_and_seq_type
    DROP external_script_version;


-- previously used for roddy configs
UPDATE
    config_per_project_and_seq_type
SET
    program_version = plugin_version
WHERE
    program_version IS NULL AND
    plugin_version IS NOT NULL;


ALTER TABLE config_per_project_and_seq_type
    DROP plugin_version;


-- programVersion can now be made NOT NULL on the database level
ALTER TABLE config_per_project_and_seq_type
    ALTER COLUMN program_version
        SET NOT NULL;