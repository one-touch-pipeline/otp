databaseChangeLog = {

    changeSet(author: "nlangh", id: "1656498064743-79") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "seq_type_id", tableName: "workflow_version_selector")
    }
}
