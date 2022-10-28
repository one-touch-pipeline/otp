databaseChangeLog = {

    changeSet(author: "", id: "1667211769098-145") {
        addUniqueConstraint(columnNames: "library_layout, dir_name", constraintName: "UK5a0d148daef7ad278ef33a7aff20", tableName: "seq_type")
    }
}
