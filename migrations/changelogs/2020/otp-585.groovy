databaseChangeLog = {

    changeSet(author: "", id: "1594988549086-1") {
        addColumn(tableName: "workflow_run") {
            column(name: "project_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1594988549086-8") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "workflow_run", constraintName: "FKa5td0udmh0u67gucn7dyk08md", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }
}
