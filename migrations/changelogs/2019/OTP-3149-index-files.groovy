databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1558603331380-1") {
        addColumn(tableName: "data_file") {
            column(name: "index_file", type: "boolean", value: "false")
        }
        addNotNullConstraint(columnDataType: "boolean", columnName: "index_file", tableName: "data_file")
    }
}
