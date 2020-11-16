databaseChangeLog = {
    changeSet(author: "", id: "1605552201693-42") {
        addNotNullConstraint(columnDataType: "clob", columnName: "combined_config", tableName: "workflow_run")
    }
}
