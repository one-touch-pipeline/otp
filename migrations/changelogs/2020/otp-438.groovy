databaseChangeLog = {
    changeSet(author: "", id: "1588180713646-3") {
        createTable(tableName: "workflow_run_input_artefact") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "role", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "workflow_artefact_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "workflow_run_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }


    changeSet(author: "", id: "1588180713646-6") {
        addPrimaryKey(columnNames: "id", constraintName: "workflow_run_input_artefactPK", tableName: "workflow_run_input_artefact")
    }

    changeSet(author: "", id: "1588180713646-10") {
        addUniqueConstraint(columnNames: "workflow_run_id, workflow_artefact_id", constraintName: "UK6cb454c8bcac6b4d804f975198d6", tableName: "workflow_run_input_artefact")
    }

    changeSet(author: "", id: "1588180713646-11") {
        addUniqueConstraint(columnNames: "workflow_run_id, role", constraintName: "UKcabae7dda3e0a9f765ad6d9d5453", tableName: "workflow_run_input_artefact")
    }

    changeSet(author: "", id: "1588180713646-12") {
        addForeignKeyConstraint(baseColumnNames: "workflow_run_id", baseTableName: "workflow_run_input_artefact", constraintName: "FK9fy0nd3op8q37e8fkralcq8gq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_run")
    }

    changeSet(author: "", id: "1588180713646-15") {
        addForeignKeyConstraint(baseColumnNames: "workflow_artefact_id", baseTableName: "workflow_run_input_artefact", constraintName: "FKmr9b61qlilhcvk5ey3nxojtx9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_artefact")
    }

    changeSet(author: "", id: "1587736571819-10") {
        dropForeignKeyConstraint(baseTableName: "workflow_artefact", constraintName: "workflow_artefact_artefact_id_fkey")
    }

    changeSet(author: "", id: "1588180713646-33") {
        dropTable(tableName: "workflow_run_input_artefacts")
    }

    changeSet(author: "", id: "1587736571819-37") {
        dropColumn(columnName: "artefact_id", tableName: "workflow_artefact")
    }
}
