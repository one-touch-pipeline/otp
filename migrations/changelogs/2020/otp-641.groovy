databaseChangeLog = {

    changeSet(author: "", id: "1597736531416-1") {
        addColumn(tableName: "external_workflow_config_selector") {
            column(name: "selector_type", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1597736531416-4") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_EXTERNAL_WORKFLOW_CONFIG_SELECTORNAME_COL", tableName: "external_workflow_config_selector")
    }
}
