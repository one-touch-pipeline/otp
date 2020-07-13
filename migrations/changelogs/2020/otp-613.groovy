databaseChangeLog = {

    changeSet(author: "", id: "1594793211780-1") {
        createTable(tableName: "workflow_job_error_definition") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "allow_restarting_count", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "job_bean_name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "bean_to_restart", type: "VARCHAR(255)")

            column(name: "source_type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "error_expression", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "mail_text", type: "TEXT")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "action", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1594793211780-2") {
        addPrimaryKey(columnNames: "id", constraintName: "workflow_job_error_definitionPK", tableName: "workflow_job_error_definition")
    }
}
