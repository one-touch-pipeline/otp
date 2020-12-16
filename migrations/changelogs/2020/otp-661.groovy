databaseChangeLog = {

    changeSet(author: "", id: "1608125788765-3") {
        addUniqueConstraint(columnNames: "restarted_from_id", constraintName: "UC_WORKFLOW_RUNRESTARTED_FROM_ID_COL", tableName: "workflow_run")
    }

    changeSet(author: "", id: "1608125788765-4") {
        addUniqueConstraint(columnNames: "restarted_from_id", constraintName: "UC_WORKFLOW_STEPRESTARTED_FROM_ID_COL", tableName: "workflow_step")
    }
}
