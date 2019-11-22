databaseChangeLog = {

    changeSet(author: "", id: "1574440440281-70") {
        dropNotNullConstraint(columnDataType: "date", columnName: "storage_until", tableName: "project")
    }
}
