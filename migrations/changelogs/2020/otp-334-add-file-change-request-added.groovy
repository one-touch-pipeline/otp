databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1581068643082-1") {
        addColumn(tableName: "user_project_role") {
            column(name: "file_access_change_requested", type: "boolean", defaultValueBoolean: "false") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka (generated)", id: "1581072448610-39") {
        dropDefaultValue(columnDataType: "boolean", columnName: "file_access_change_requested", tableName: "user_project_role")
    }
}
