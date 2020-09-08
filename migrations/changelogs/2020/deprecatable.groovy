databaseChangeLog = {

    changeSet(author: "", id: "1599575214148-41") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "comment_id", tableName: "external_workflow_config_fragment")
    }

    changeSet(author: "", id: "1599575214148-43") {
        dropNotNullConstraint(columnDataType: "date", columnName: "deprecation_date", tableName: "active_project_workflow")
    }

    changeSet(author: "", id: "1599575214148-44") {
        dropNotNullConstraint(columnDataType: "date", columnName: "deprecation_date", tableName: "external_workflow_config_fragment")
    }
}
