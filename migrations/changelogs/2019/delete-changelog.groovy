databaseChangeLog = {

    changeSet(author: "", id: "1568819590277-1") {
        addColumn(tableName: "data_file") {
            column(name: "withdrawn_comment", type: "varchar(255)")
        }
    }

    changeSet(author: "", id: "1568819590277-2") {
        addColumn(tableName: "data_file") {
            column(name: "withdrawn_date", type: "timestamp")
        }
    }

    changeSet(author: "", id: "1568819590277-6") {
        dropForeignKeyConstraint(baseTableName: "change_log", constraintName: "fk80f28e35c87ce5ee")
    }

    changeSet(author: "", id: "1568819590277-15") {
        dropUniqueConstraint(constraintName: "referenced_class_class_name_key", tableName: "referenced_class")
    }

    changeSet(author: "", id: "1568819590277-19") {
        dropTable(tableName: "change_log")
    }

    changeSet(author: "", id: "1568819590277-20") {
        dropTable(tableName: "referenced_class")
    }
}
