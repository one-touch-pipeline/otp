databaseChangeLog = {

    changeSet(author: "", id: "1666865218451-77") {
        addColumn(tableName: "document") {
            column(name: "link", type: "varchar(255)")
        }
    }

    changeSet(author: "", id: "1666865218451-5") {
        dropNotNullConstraint(columnDataType: "bytea", columnName: "content", tableName: "document")
    }
}
