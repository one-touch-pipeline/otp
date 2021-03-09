databaseChangeLog = {
    changeSet(author: "", id: "1615306640168-3") {
        addColumn(tableName: "fastqc_processed_file") {
            column(name: "workflow_artefact_id", type: "int8")
        }
    }

    changeSet(author: "", id: "1615306640168-8") {
        addForeignKeyConstraint(baseColumnNames: "workflow_artefact_id", baseTableName: "fastqc_processed_file", constraintName: "FKoqfd6tyh64wy6jfr7nuf4ueye", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_artefact")
    }
}
