databaseChangeLog = {

    changeSet(author: "", id: "1624867830354-1") {
        addColumn(tableName: "abstract_field_definition") {
            column(name: "regular_expression_error", type: "varchar(255)")
        }
    }
}
