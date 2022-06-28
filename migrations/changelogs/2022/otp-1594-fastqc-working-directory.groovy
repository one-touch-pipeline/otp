databaseChangeLog = {

    changeSet(author: "", id: "1654777892156-1") {
        addColumn(tableName: "fastqc_processed_file") {
            column(name: "work_directory_name", type: "varchar(255)") {
                constraints(nullable: "true")
            }
        }
    }
    changeSet(author: "", id: "1654777892156-2") {
        sql("update fastqc_processed_file set work_directory_name = 'bash-unknown-version';")
    }
    changeSet(author: "", id: "1654777892156-3") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "work_directory_name", tableName: "fastqc_processed_file")
    }
}
