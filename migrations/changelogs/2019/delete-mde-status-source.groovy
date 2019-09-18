databaseChangeLog = {

    changeSet(author: "", id: "1568819984072-32") {
        dropColumn(columnName: "source", tableName: "meta_data_entry")
    }

    changeSet(author: "", id: "1568819984072-33") {
        dropColumn(columnName: "status", tableName: "meta_data_entry")
    }
}
