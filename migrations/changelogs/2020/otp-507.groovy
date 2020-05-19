databaseChangeLog = {
    changeSet(author: "", id: "1589807529559-1") {
        addColumn(tableName: "workflow_run") {
            column(name: "priority_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1589807529559-4") {
        createIndex(indexName: "workflow_run_priority_idx", tableName: "workflow_run") {
            column(name: "priority_id")
        }
    }

    changeSet(author: "", id: "1589807529559-5") {
        addForeignKeyConstraint(baseColumnNames: "priority_id", baseTableName: "workflow_run", constraintName: "FKmcntfdirb0cvtbkq6wmrqxpcw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "processing_priority")
    }

    changeSet(author: "", id: "1589807529559-40") {
        dropColumn(columnName: "priority", tableName: "workflow_run")
    }
}
