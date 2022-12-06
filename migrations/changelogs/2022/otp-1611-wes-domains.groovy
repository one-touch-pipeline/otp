databaseChangeLog = {

    changeSet(author: "", id: "1670489633776-76") {
        createTable(tableName: "wes_log") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "wes_logPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "end_time", type: "TIMESTAMP WITHOUT TIME ZONE")

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "stdout", type: "TEXT")

            column(name: "stderr", type: "TEXT")

            column(name: "cmd", type: "TEXT")

            column(name: "exit_code", type: "INTEGER")

            column(name: "start_time", type: "TIMESTAMP WITHOUT TIME ZONE")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1670489633776-77") {
        createTable(tableName: "wes_run_log") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "wes_run_logPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "workflow_step_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "run_log_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "run_request", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "", id: "1670489633776-78") {
        createTable(tableName: "wes_run_log_wes_log") {
            column(name: "wes_run_log_task_logs_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "wes_log_id", type: "BIGINT")

            column(name: "task_logs_idx", type: "INTEGER")
        }
    }

    changeSet(author: "", id: "1670489633776-174") {
        addForeignKeyConstraint(baseColumnNames: "workflow_step_id", baseTableName: "wes_run_log", constraintName: "FK3geplj7dns1dc5e8wifj9p59f", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step", validate: "true")
    }

    changeSet(author: "", id: "1670489633776-175") {
        addForeignKeyConstraint(baseColumnNames: "run_log_id", baseTableName: "wes_run_log", constraintName: "FKmt47xgvoyfiduakxh4pl2u5y5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "wes_log", validate: "true")
    }

    changeSet(author: "", id: "1670489633776-176") {
        addForeignKeyConstraint(baseColumnNames: "wes_log_id", baseTableName: "wes_run_log_wes_log", constraintName: "FKogq6dnt82emgwdsdt4srcbjkn", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "wes_log", validate: "true")
    }
}
