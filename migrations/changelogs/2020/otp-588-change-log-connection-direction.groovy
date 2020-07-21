databaseChangeLog = {

    changeSet(author: "", id: "1595260868395-1") {
        addColumn(tableName: "workflow_log") {
            column(name: "workflow_step_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1595260868395-4") {
        addForeignKeyConstraint(baseColumnNames: "workflow_step_id", baseTableName: "workflow_log", constraintName: "FKpmkyuwhu92v5g0ytbobqicdq9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step")
    }

    changeSet(author: "", id: "1595260868395-25") {
        dropTable(tableName: "workflow_step_workflow_log")
    }
}
