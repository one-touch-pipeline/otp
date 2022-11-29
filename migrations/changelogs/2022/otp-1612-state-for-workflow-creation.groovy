databaseChangeLog = {

    changeSet(author: "", id: "1669813380810-76") {
        addColumn(tableName: "fastq_import_instance") {
            column(name: "state", type: "varchar(255)") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "", id: "1669813380810-76b") {
        sql("update fastq_import_instance set state = 'SUCCESS';")
    }

    changeSet(author: "", id: "1669813380810-76c") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "state", tableName: "fastq_import_instance")
    }

    changeSet(author: "", id: "1669813380810-172") {
        createIndex(indexName: "fastq_import_instance_state_idx", tableName: "fastq_import_instance") {
            column(name: "state")
        }
    }
}
